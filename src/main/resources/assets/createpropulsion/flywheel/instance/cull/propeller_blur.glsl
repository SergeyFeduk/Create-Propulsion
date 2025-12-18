void flw_transformBoundingSphere(in FlwInstance instance, inout vec3 center, inout float radius) {
    //Instance is offset by 0.5!!!
    radius += length(center - 0.5);
    center += instance.pos;
}