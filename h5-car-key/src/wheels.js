import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';

// Five split-Y cast spokes, narrow machined faces and recessed black webs,
// matched to the user's side-on wheel photo. Axle and tire positions stay in
// the supplied body's measured wheel arches (2.73 m wheelbase).
export function createWheels() {
  const root = new THREE.Group(); root.name = 'Reference_split_Y_wheels';
  const rubber = new THREE.MeshStandardMaterial({ color: 0x17191c, roughness: .88 });
  const tread = new THREE.MeshStandardMaterial({ color: 0x090a0c, roughness: .96 });
  const alloy = new THREE.MeshStandardMaterial({ color: 0xd0d2d5, metalness: .84, roughness: .29 });
  const graphite = new THREE.MeshStandardMaterial({ color: 0x111317, metalness: .48, roughness: .3 });
  const brake = new THREE.MeshStandardMaterial({ color: 0x393a3c, metalness: .75, roughness: .64, envMapIntensity: .35 });
  const caliper = new THREE.MeshStandardMaterial({ color: 0x85888b, metalness: .5, roughness: .48 });
  const add = (group, geometry, material, position = [0, 0, 0], rotation = [0, 0, 0]) => {
    const mesh = new THREE.Mesh(geometry, material); mesh.position.set(...position); mesh.rotation.set(...rotation); group.add(mesh); return mesh;
  };
  // The black casting is wider than its machined silver face, leaving a dark
  // bevel around each tapered arm instead of round, uniformly silver rods.
  const casting = [[-.016,.057],[-.026,.10],[-.076,.230],[-.080,.241],[-.064,.249],[-.057,.226],[-.012,.122],[0,.111],[.012,.122],[.057,.226],[.064,.249],[.080,.241],[.076,.230],[.026,.10],[.016,.057]];
  const facing = [[-.011,.064],[-.020,.10],[-.070,.233],[-.072,.242],[-.065,.244],[-.059,.226],[-.010,.119],[0,.109],[.010,.119],[.059,.226],[.065,.244],[.072,.242],[.070,.233],[.020,.10],[.011,.064]];
  function spokeGeometry(points, solid = false) {
    const shape = new THREE.Shape(points.map(([x,y]) => new THREE.Vector2(x,y)));
    const geometry = solid ? new THREE.ExtrudeGeometry(shape, { depth: .014, bevelEnabled: true, bevelSegments: 1, steps: 1, bevelSize: .0012, bevelThickness: .001 }) : new THREE.ShapeGeometry(shape);
    const position = geometry.attributes.position;
    for (let i=0; i<position.count; i++) {
      const radius = Math.hypot(position.getX(i), position.getY(i));
      position.setZ(i, position.getZ(i) + .024 * THREE.MathUtils.clamp((radius-.055)/.2,0,1));
    }
    geometry.computeVertexNormals();
    return geometry;
  }
  const logoCanvas = document.createElement('canvas'); logoCanvas.width=256; logoCanvas.height=256;
  const ctx=logoCanvas.getContext('2d');
  ctx.fillStyle='#14161a';ctx.fillRect(0,0,256,256);
  ctx.fillStyle='#d8dadd';ctx.font='500 29px Arial, sans-serif';ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText('LYNK & CO',128,128);
  const logoTexture=new THREE.CanvasTexture(logoCanvas);logoTexture.colorSpace=THREE.SRGBColorSpace;
  const badge = new THREE.MeshStandardMaterial({ map:logoTexture, metalness:.3, roughness:.38 });
  const tireProfile = [[.253,-.103],[.288,-.118],[.321,-.107],[.339,-.078],[.346,-.035],[.347,0],[.346,.035],[.339,.078],[.321,.107],[.288,.118],[.253,.103],[.253,-.103]].map(([x,y]) => new THREE.Vector2(x,y));
  const wheels = [];
  for (const side of [-1, 1]) for (const axle of [1.312, -1.418]) {
    const wheel = new THREE.Group(); wheel.position.set(side * .800, .351, axle); wheel.rotation.y = side * Math.PI / 2; root.add(wheel);
    wheels.push(wheel);
    add(wheel,new THREE.LatheGeometry(tireProfile,96),rubber,[0,0,0],[Math.PI/2,0,0]);
    for (const depth of [-.042,.042]) add(wheel,new THREE.TorusGeometry(.346,.0015,4,96),tread,[0,0,depth]);
    for (let i=0;i<72;i++) {
      const angle=i/72*Math.PI*2;
      for (const band of [-1,1]) {
        const mark=add(wheel,new THREE.BoxGeometry(.0015,.026,.018),tread,[Math.cos(angle)*.336,Math.sin(angle)*.336,band*.073]);
        mark.rotation.z=angle;
      }
    }
    // Bring the wheel face closer to the arch without exposing the silver lip
    // beyond the tire shoulder: outer rim <= .108 m, tire sidewall .118 m.
    const rim = new THREE.Group(); rim.position.z = -.038; wheel.add(rim);
    // Open barrel and recessed brake disc remain visible through the spokes.
    add(rim,new THREE.CylinderGeometry(.252,.245,.15,96,1,true),graphite,[0,0,.035],[Math.PI/2,0,0]);
    add(rim,new THREE.RingGeometry(.228,.251,96),graphite,[0,0,-.039]);
    add(rim,new THREE.CylinderGeometry(.200,.200,.012,64),brake,[0,0,.070],[Math.PI/2,0,0]);
    for(const radius of [.145,.174,.195]) add(rim,new THREE.TorusGeometry(radius,.00055,4,64),graphite,[0,0,.0765]);
    add(rim,new THREE.CylinderGeometry(.071,.071,.024,32),graphite,[0,0,.087],[Math.PI/2,0,0]);
    add(rim,new THREE.TorusGeometry(.253,.006,8,96),graphite,[0,0,.131]);
    add(rim,new THREE.TorusGeometry(.254,.0033,8,96),alloy,[0,0,.142]);
    add(rim,new THREE.BoxGeometry(.044,.104,.026),caliper,[-.164,.025,.084]);
    add(rim,new THREE.BoxGeometry(.029,.065,.012),caliper,[-.180,.025,.101]);
    for(let i=0;i<5;i++) {
      const angle=i/5*Math.PI*2;
      const body=add(rim,spokeGeometry(casting,true),graphite,[0,0,.106]);body.rotation.z=angle;
      const face=add(rim,spokeGeometry(facing),alloy,[0,0,.122]);face.rotation.z=angle;
      const web=add(rim,spokeGeometry([[-.006,.116],[-.010,.246],[.010,.246],[.006,.116]],true),graphite,[0,0,.090]);web.rotation.z=angle;
      const tab=add(rim,spokeGeometry([[-.010,.242],[-.007,.249],[.007,.249],[.010,.242],[0,.235]]),alloy,[0,0,.122]);tab.rotation.z=angle;
      const boltAngle=angle+Math.PI/2+Math.PI/5;
      const x=Math.cos(boltAngle)*.052,y=Math.sin(boltAngle)*.052;
      add(rim,new THREE.CylinderGeometry(.010,.010,.009,16),tread,[x,y,.122],[Math.PI/2,0,0]);
      add(rim,new THREE.CylinderGeometry(.0055,.0055,.009,6),alloy,[x,y,.127],[Math.PI/2,0,0]);
    }
    add(rim,new THREE.CylinderGeometry(.040,.044,.022,48),graphite,[0,0,.120],[Math.PI/2,0,0]);
    add(rim,new THREE.TorusGeometry(.039,.0015,6,48),alloy,[0,0,.133]);
    add(rim,new THREE.CircleGeometry(.0375,48),badge,[0,0,.135]);
    const valveAngle=.52;
    add(rim,new THREE.CylinderGeometry(.0035,.004,.022,8),tread,[Math.cos(valveAngle)*.238,Math.sin(valveAngle)*.238,.116],[Math.PI/2,0,0]);
  }
  root.updateMatrixWorld(true);
  // Keep the measured tire contacts before merging removes individual wheels.
  root.userData.groundContacts = wheels.map(wheel => ({
    x: wheel.position.x, z: wheel.position.z, y: new THREE.Box3().setFromObject(wheel).min.y,
  }));
  const buckets=new Map();
  root.traverse(mesh=>{if(!mesh.isMesh)return; const geometry=(mesh.geometry.index ? mesh.geometry.toNonIndexed() : mesh.geometry.clone()).applyMatrix4(mesh.matrixWorld); const list=buckets.get(mesh.material)||[];list.push(geometry);buckets.set(mesh.material,list);mesh.geometry.dispose();});
  root.clear();
  for(const [material,geometries] of buckets){const merged=mergeGeometries(geometries,false);geometries.forEach(g=>g.dispose());root.add(new THREE.Mesh(merged,material));}
  root.userData.pattern='five split-Y spokes with machined faces and black center webs';
  return root;
}
