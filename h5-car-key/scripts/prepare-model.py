"""Prepare the user-supplied Lynk 03 Blender model for the H5 preview.
Run Blender --background --disable-autoexec SOURCE.blend --python this_file.
The source archive stays in .qa; only the assembled, articulated GLB is shipped.
"""
import bpy, bmesh, math, json
from pathlib import Path
from mathutils import Vector
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'public' / 'vehicle'
OUT.mkdir(parents=True, exist_ok=True)
FINISH = json.loads((ROOT/'src'/'vehicle-finish.json').read_text())

def bounds(o):
    pts = [o.matrix_world @ Vector(p) for p in o.bound_box]
    return Vector([min(p[i] for p in pts) for i in range(3)]), Vector([max(p[i] for p in pts) for i in range(3)])

# The main assembled car is centered at x=0. Alternate kits were laid out
# beside it, spare bumpers behind it, and a spare roof above it.
keep = []
# Replace the source outlet assemblies with a matched hollow pair; remove
# unused pipe variants. Their original axis and bumper cutouts are retained.
replaced_exhaust = {'5350', '5359', '1416', '1433'}
# The centered assembly wears the fog-light/aero bumper. The user's photo
# instead matches the stock bumper parked to the left in the source scene.
stock_front = {'4588', '4600', '4612'}
replaced_front = {'5234','573','5873','5882','5891','5900','5864',
                  '5591','5606','5621','5639'}
# Optional right-fender vent laid over the complete painted fender. The
# reference car has a continuous body panel, so omit both trim and insert.
removed_side_trim = {'5408','5417'}
# Keep the regular 03 badges shown in the rear reference. The archive also
# stacks 03+, PHEV and CYAN variants on the same trunk.
removed_badges = {'2590','2602','2624','2636','5277','5436'}
rear_badges = {'2572','4773','5669'}
for name in stock_front:
    bpy.data.objects[name].location.x += 2.689
bpy.context.view_layer.update()
for o in list(bpy.data.objects):
    if o.type != 'MESH':
        bpy.data.objects.remove(o, do_unlink=True)
        continue
    lo, hi = bounds(o)
    c = (lo + hi) / 2
    if o.name not in replaced_exhaust | replaced_front | removed_side_trim | removed_badges and abs(c.x) < 1.10 and lo.z > -.05 and hi.z < 1.60 and lo.y > -2.55 and hi.y < 2.55:
        keep.append(o)
    else:
        bpy.data.objects.remove(o, do_unlink=True)

# All texture slots in this archive are empty. Rebuild a local PBR palette.
def material(name, color, metal=0, rough=.5, alpha=1, coat=0):
    finish = FINISH.get(name, {})
    color = finish.get('color', color)
    metal = finish.get('metalness', metal)
    rough = finish.get('roughness', rough)
    alpha = finish.get('opacity', alpha)
    coat = finish.get('clearcoat', coat)
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    p = m.node_tree.nodes.get('Principled BSDF')
    p.inputs['Base Color'].default_value = (*color, alpha)
    p.inputs['Metallic'].default_value = metal
    p.inputs['Roughness'].default_value = rough
    p.inputs['Alpha'].default_value = alpha
    p.inputs['Coat Weight'].default_value = coat
    p.inputs['Coat Roughness'].default_value = finish.get('clearcoatRoughness', .14)
    m.diffuse_color = (*color, alpha)
    m.use_backface_culling = False
    if alpha < 1: m.surface_render_method = 'BLENDED'
    return m
paint = material('Graphite gray metallic', (.105,.12,.15), .52,.34,coat=.32)
blackpaint = material('Gloss black trim',(.012,.016,.021),.35,.22,coat=.35)
metal = material('Brushed alloy',(.42,.46,.50),.88,.25)
chrome = material('Polished chrome',(.68,.72,.76),.95,.16)
badge_chrome = material('Rear badge bright silver',(.72,.75,.78),.78,.2,coat=.35)
rear_garnish = material('Rear center gloss black',(.004,.005,.006),.2,.17,coat=.5)
rubber = material('Rubber',(.014,.018,.024),0,.86)
plastic = material('Dark interior',(.022,.026,.031),.08,.62)
fabric = material('Seat fabric',(.032,.038,.046),0,.95)
glass = material('Tinted automotive glass',(.026,.044,.064),.32,.16,.76,coat=.8)
lens = material('Headlight lens',(.42,.48,.57),.45,.12,.82)
red = material('Tail light lens',(.40,.004,.009),.35,.23)
white = material('LED running light',(.80,.89,1),.1,.25)
stitch = material('Interior stitching',(.15,.16,.17),0,.8)
exhaust = material('Satin stainless exhaust',(.22,.24,.27),.9,.29)
diffuser = material('Rear diffuser',(.012,.015,.02),.05,.55)
tail_cover = material('Taillight smoked cover',(.07,.075,.08),.05,.16,.08,coat=.35)
tail_optic = material('Taillight clear optics',(.65,.68,.7),.05,.18,.1,coat=.25)
tail_red_optic = material('Taillight red perimeter lens',(.5,.002,.009),.05,.2,.32,coat=.4)
tail_lower_reflector = material('Taillight lower silver reflector',(.48,.51,.54),.68,.24)
tail_housing = material('Taillight black housing',(.003,.003,.004),.25,.23,coat=.4)
tail_reflector = material('Taillight dark reflector',(.045,.018,.022),.7,.24)
tail_led = material('Taillight LED blades',(.68,.72,.76),.25,.27)
tail_loop = material('Taillight LED lower loop',(.55,.002,.009),.1,.24,coat=.35)
for m in (tail_led,tail_loop):
    p=m.node_tree.nodes.get('Principled BSDF')
    p.inputs['Emission Color'].default_value=(1,.006,.016,1)
    p.inputs['Emission Strength'].default_value=1.35

# The original model already contains the paired energy-crystal blades and
# folded lower light guides. Transparent covers must not hide these meshes.
tail_parts = {
    'cover': {'1905','1923','2181','2199'},
    'optics': {'2001','2115','2229','2311'},
    'redPerimeter': {'2013','2127'},
    'lowerReflector': {'1986','2103'},
    'blades': {'1941','2067','2241','2323'},
    'lowerLoop': {'1956','2079'},
    'housing': {'1971','2091','2253','2335'},
    'reflector': {'2025','2139','2217','2299','2265','2347'},
}
tail_materials = {'cover':tail_cover,'optics':tail_optic,'blades':tail_led,
                  'lowerLoop':tail_loop,'housing':tail_housing,'reflector':tail_reflector,
                  'redPerimeter':tail_red_optic,'lowerReflector':tail_lower_reflector}
tail_material_by_part = {part:tail_materials[role] for role,parts in tail_parts.items() for part in parts}

front_cover = material('Headlight clear cover',(.18,.20,.23),.02,.12,.08,coat=.4)
front_optic = material('Headlight inner optics',(.24,.28,.32),.05,.16,.12)
front_housing = material('Headlight black housing',(.003,.004,.005),.2,.28,coat=.3)
front_reflector = material('Headlight reflector',(.18,.20,.23),.92,.22)
projector_glass = material('Headlight projector glass',(.09,.13,.17),.45,.1,coat=.65)
front_grille = material('Front grille satin black',(.009,.011,.014),.25,.36)
front_drl = material('Headlight DRL blades',(.82,.88,1),.05,.25)
p=front_drl.node_tree.nodes.get('Principled BSDF')
p.inputs['Emission Color'].default_value=(.84,.91,1,1)
p.inputs['Emission Strength'].default_value=2
front_parts = {
    'cover': {'2874','2892','654','714'},
    'optics': {'2946','3015'},
    'housing': {'2910','2928','604','634'},
    'reflector': {'591','624','2985','3051','2958','3027'},
    'drl': {'2970','3039'},
    'grille': {'555','4588','4612'},
}
front_materials = {'cover':front_cover,'optics':front_optic,'housing':front_housing,
                   'reflector':front_reflector,'drl':front_drl,'grille':front_grille}
front_material_by_part = {part:front_materials[role] for role,parts in front_parts.items() for part in parts}

# The archive assigned bare metal to the black diffuser and underbody fins.
# These surfaces sit beside the exhaust, so their silver reflections made the
# left outlet appear to blend into an irregular protruding bumper.
rear_black_parts = {'5466', '1589', '2557', '5824'}

# Identify the actual glass surfaces in this archive (all were assigned the
# generic metal material). The surrounding door frames remain stationary.
windows = {'675':'Window_front_left','732':'Window_front_right',
           '3929':'Window_rear_left','3947':'Window_rear_right'}
mirror_parts = {
    'left': {'4857','4869','4881','4893','4905','4917'},
    'right': {'4955','4967','4979','4991','5003','5015'},
}
fixed_glass = {'1607','1639','1625','3099','2520','3081','423','81'}
for o in keep:
    original = o.name
    lo, hi = bounds(o)
    interior = lo.x > -.73 and hi.x < .73 and lo.y > -1.10 and hi.y < 1.82 and lo.z > .30 and hi.z < 1.36
    for slot in o.material_slots:
        n = slot.material.name if slot.material else ''
        if original in tail_material_by_part: m = tail_material_by_part[original]
        elif original in rear_badges: m = badge_chrome
        elif original in {'239','5178'}: m = rear_garnish
        elif original in front_material_by_part: m = front_material_by_part[original]
        elif original in rear_black_parts: m = diffuser
        elif original in {'4905','5003'}: m = chrome
        elif original in windows or original in fixed_glass: m = glass
        elif 'carPaint_000' in n: m = blackpaint
        elif 'carPaint' in n: m = paint
        elif 'ID10_metal' in n: m = plastic if interior else metal
        elif 'chrome' in n: m = chrome
        elif any(t in n for t in ['fabric','seatl']): m = fabric
        elif 'stitch' in n: m = stitch
        elif any(t in n for t in ['tail','glass_red','reflector','blackGlass_brake']): m = red
        elif 'drl' in n or 'hibe' in n: m = white
        elif 'gls_clear' in n or 'lights_' in n or 'foglight' in n: m = red if lo.y > 1.9 else lens
        elif 'rubber' in n or 'undercarriage' in n: m = rubber
        else: m = plastic
        slot.material = m
    # Bake transforms so authored pivots below are in meters.
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    o.select_set(False)
    if original == '1450':
        # A long single-outlet system reaches below the left bumper in this
        # multi-kit archive. Keep its concealed underbody section, trimming
        # only the incompatible rear branch before the new paired outlets.
        bm = bmesh.new(); bm.from_mesh(o.data)
        bmesh.ops.bisect_plane(bm, geom=list(bm.verts)+list(bm.edges)+list(bm.faces),
            plane_co=(0,1.95,0), plane_no=(0,1,0), dist=.00001,
            clear_outer=True, clear_inner=False)
        bm.to_mesh(o.data); bm.free(); o.data.update()
    o['source_part'] = original
    if original in windows:
        o.name = windows[original]
        o['motion'] = 'window'
    elif original in fixed_glass:
        o.name = 'Glass_fixed_' + original

# The archive has only broad ribbed reflector surfaces in the main lights.
# Add the four small optical modules visible behind each clear outer lens.
for side in (-1,1):
    for column,x in enumerate((.567,.700)):
        y=-1.992+(x-.567)*.69
        for row,z in enumerate((.632,.675)):
            tag=f'Projector_{side}_{column}_{row}'
            def optical_block(suffix,center,half_size,mat,bevel):
                bpy.ops.mesh.primitive_cube_add(size=2,location=center)
                obj=bpy.context.object;obj.name=tag+suffix;obj.scale=half_size
                bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
                edge=obj.modifiers.new('Soft optical edge','BEVEL');edge.width=bevel;edge.segments=3
                bpy.context.view_layer.objects.active=obj
                bpy.ops.object.modifier_apply(modifier=edge.name)
                obj.data.materials.append(mat)
                bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
                obj['source_part']=tag+suffix;keep.append(obj)
                return obj
            optical_block('_recess',(side*x,y+.01,z),(.034,.014,.019),front_housing,.005)
            optical_block('_bezel',(side*x,y,z),(.030,.006,.014),front_reflector,.003)
            optical_block('_lens',(side*x,y-.006,z),(.026,.004,.010),projector_glass,.003)
            optical_block('_facet',(side*x,y-.0105,z+.002),(.023,.001,.002),chrome,.0008)

for side, x in [('left', -.60), ('right', .60)]:
    # Six rings make a small rolled lip, an open bore, and a recessed dark
    # interior. Both sides share exactly the same radius, length and height.
    rings = [(2.20,.050),(2.360,.050),(2.366,.048),
             (2.366,.043),(2.360,.042),(2.20,.042)]
    count = 64
    vertices = [(x + radius*math.cos(i*2*math.pi/count), y,
                 .327 + radius*math.sin(i*2*math.pi/count))
                for y,radius in rings for i in range(count)]
    faces = [(j*count+i,j*count+(i+1)%count,(j+1)*count+(i+1)%count,(j+1)*count+i)
             for j in range(len(rings)-1) for i in range(count)]
    mesh = bpy.data.meshes.new('Exhaust_' + side)
    mesh.from_pydata(vertices,[],faces); mesh.materials.append(exhaust);mesh.materials.append(rubber)
    bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free()
    for polygon in mesh.polygons:
        polygon.use_smooth=True
        if polygon.index >= 4*count: polygon.material_index=1
    obj=bpy.data.objects.new('Exhaust_' + side,mesh);bpy.context.collection.objects.link(obj)
    obj['source_part']='Rebuilt hollow exhaust ' + side;keep.append(obj)
    bpy.ops.mesh.primitive_circle_add(vertices=count,radius=.042,fill_type='NGON',
        location=(x,2.21,.327),rotation=(math.pi/2,0,0))
    cap=bpy.context.object;cap.name='Exhaust_inner_' + side;cap.data.materials.append(rubber)
    bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
    cap['source_part']='Recessed exhaust bore ' + side;keep.append(cap)

# The trunk lid is already independent; collect its skin, underside, badge,
# latch and attached spoiler. This sedan lid pivots near the rear screen.
trunk = bpy.data.objects.new('Trunk_hinge',None)
bpy.context.collection.objects.link(trunk)
trunk.location=(0,1.815,1.095)
trunk['motion']='trunk'
bpy.context.view_layer.update()
trunk_parts=[]
for o in keep:
    lo,hi=bounds(o)
    if lo.y >= 1.805 and hi.y <= 2.41 and lo.z >= .65 and hi.z <= 1.15 and lo.x > -.66 and hi.x < .66:
        o.parent=trunk
        o.location=-trunk.location
        trunk_parts.append(o['source_part'])

# Keep each complete mirror housing (cap, mirror surface, trim and sensor)
# separate from the fixed door mount. The fold axis is vertical at its base.
for side, names in mirror_parts.items():
    hinge = bpy.data.objects.new('Mirror_' + side + '_hinge', None)
    bpy.context.collection.objects.link(hinge)
    hinge.location = (-.873 if side == 'left' else .873, -.557, 1.015)
    hinge['motion'] = 'mirror'
    hinge['side'] = side
    bpy.context.view_layer.update()
    for o in keep:
        if o.get('source_part') in names:
            o.parent = hinge
            o.location = -hinge.location

# Merge by material within each motion group. Four glasses and three sets of
# articulated parts retain independent transforms.
groups = {}
for o in keep:
    if o.get('motion') == 'window': continue
    key = (o.parent.name if o.parent else '', o.data.materials[0].name)
    groups.setdefault(key, []).append(o)
for (parent, mat), objects in groups.items():
    if len(objects) < 2: continue
    bpy.ops.object.select_all(action='DESELECT')
    for o in objects: o.select_set(True)
    bpy.context.view_layer.objects.active = objects[0]
    source_parts = [o.get('source_part') for o in objects]
    bpy.ops.object.join()
    merged = bpy.context.view_layer.objects.active
    prefix = 'Lid_' if parent == 'Trunk_hinge' else (parent + '_' if parent else 'Body_')
    merged.name = prefix + mat.replace(' ', '_')
    merged['source_parts'] = source_parts

bpy.ops.object.select_all(action='SELECT')
bpy.ops.export_scene.gltf(filepath=str(OUT/'lynk03.glb'),export_format='GLB',
    use_selection=True,export_extras=True,export_yup=True,export_animations=False,
    export_texcoords=False,export_tangents=False,
    export_draco_mesh_compression_enable=True,export_draco_mesh_compression_level=6)
meta={'name':'LYNK & CO 03','color':'gray','mode':'articulated-model',
      'source':'User-supplied lynk03.rar','windows':list(windows.values()),
      'trunkParts':trunk_parts,'mirrorParts':{side:sorted(names) for side,names in mirror_parts.items()},
      'sourceMeshCount':len(keep),'textures':'Rebuilt PBR materials; original archive contains no texture images'}
meta['finish']='APP car_render.png reference: neutral steel gray, dark glazing, neutral studio reflections'
meta['taillights']={role:sorted(parts) for role,parts in tail_parts.items()}
meta['rearBadges']={'silver':sorted(rear_badges),'removedVariants':sorted(removed_badges)}
meta['front']={'stockBumperParts':sorted(stock_front),'removedParts':sorted(replaced_front),
               'lamps':{role:sorted(parts) for role,parts in front_parts.items()},'projectorModulesPerSide':4}
meta['side']={'removedVentParts':sorted(removed_side_trim),'fenders':['3214','3232']}
meta['exhaust']={'outlets':2,'radius':.050,'height':.327,'centersX':[-.60,.60],'replacedSourceParts':sorted(replaced_exhaust),'trimmedUnderbodyPart':'1450'}
(OUT/'model.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2))
print('ASSEMBLED',len(keep),'meshes; trunk parts:',trunk_parts)
