import { readFile } from 'node:fs/promises';
import { Matrix4, Quaternion, Vector3, Box3 } from 'three';

// Inspect a downloaded glTF/GLB before choosing movable parts and their pivots.
const file = process.argv[2];
if (!file) throw new Error('Usage: node scripts/inspect-model.mjs path/to/model.glb');
const raw = await readFile(file);
let data;
if (raw.toString('utf8', 0, 4) === 'glTF') {
  if (raw.readUInt32LE(4) !== 2) throw new Error('Only glTF 2.0 is supported');
  let offset = 12;
  while (offset + 8 <= raw.length) {
    const length = raw.readUInt32LE(offset), type = raw.readUInt32LE(offset + 4);
    if (type === 0x4e4f534a) { data = JSON.parse(raw.toString('utf8', offset + 8, offset + 8 + length).trim()); break; }
    offset += 8 + length;
  }
} else data = JSON.parse(raw.toString('utf8'));
if (!data) throw new Error('Missing glTF JSON chunk');
const meshes = [];
const allBounds = new Box3();
function visit(index, parentMatrix, path) {
  const node = data.nodes[index];
  const local = node.matrix ? new Matrix4().fromArray(node.matrix) : new Matrix4().compose(
    new Vector3(...(node.translation || [0, 0, 0])),
    new Quaternion(...(node.rotation || [0, 0, 0, 1])),
    new Vector3(...(node.scale || [1, 1, 1])),
  );
  const world = parentMatrix.clone().multiply(local);
  const nodePath = [...path, node.name || `node-${index}`];
  if (node.mesh !== undefined) {
    const mesh = data.meshes[node.mesh];
    const bounds = new Box3();
    const materials = new Set();
    let vertices = 0, triangles = 0;
    for (const primitive of mesh.primitives) {
      const positions = data.accessors[primitive.attributes.POSITION];
      if (positions.min && positions.max) bounds.union(new Box3(new Vector3(...positions.min), new Vector3(...positions.max)).applyMatrix4(world));
      vertices += positions.count;
      triangles += (primitive.indices !== undefined ? data.accessors[primitive.indices].count : positions.count) / 3;
      materials.add(data.materials?.[primitive.material]?.name || String(primitive.material));
    }
    allBounds.union(bounds);
    const round = v => v.toArray().map(x => Math.round(x * 10000) / 10000);
    meshes.push({ index, mesh: node.mesh, name: node.name, path: nodePath.join('/'), materials: [...materials], vertices, triangles: Math.round(triangles), min: round(bounds.min), max: round(bounds.max), center: round(bounds.getCenter(new Vector3())) });
  }
  for (const child of node.children || []) visit(child, world, nodePath);
}
for (const root of data.scenes[data.scene || 0].nodes) visit(root, new Matrix4(), []);
console.log(JSON.stringify({ generator: data.asset.generator, extensions: data.extensionsUsed, bounds: { min: allBounds.min.toArray(), max: allBounds.max.toArray() }, meshCount: meshes.length, animations: (data.animations || []).map(a => ({ name: a.name, channels: a.channels.map(c => ({ node: data.nodes[c.target.node].name, path: c.target.path })) })), materials: (data.materials || []).map((m, i) => ({ index: i, name: m.name, alphaMode: m.alphaMode, pbr: m.pbrMetallicRoughness })), meshes }, null, 2));
