
uniform sampler2D shadowMap; 

const float shadowoffset = 0.5;
const float shadowintensity = 2.0;
const float specularexponent = 10.0;
varying vec3 N, L;  // surface normal in camera NDC
varying vec3 v;  // surface fragment location in camera NDC
varying vec4 vL; // surface fragment location in light view NDC
 
void main(void) {

	// TODO: Compute the illumination and the shadow by comparing
	// the fragment light depth and compare with shadow map!
    vec4 vL = vL / vL.w ;
    //glPolygonOffset emulation ...
    vL.z += shadowoffset;
    vL.xy = vL.xy * 0.5 + 0.5;
    float distanceFromLight = texture2D(shadowMap,vL.xy).r;
    float shadow = 1.0;
    if (vL.w > 0.0) {
        shadow = distanceFromLight < vL.z ? shadowintensity : 1.0 ;
    }
    vec4 fragmentcolor = vec4(0.0,0.0,0.0,1.0);
    vec3 NL = normalize(gl_LightSource[0].position.xyz - v);  //normalized L
    vec3 NV = normalize(v);
    vec3 NH = normalize(NL + NV); //normalized H
    float NdotL = max(0.0, dot(N, NL));  //diffuse
    
    
    //accumulate the diffuse contributions
    fragmentcolor += gl_FrontLightProduct[0].diffuse * NdotL;  //k*I*dotProduct
   
   
    //accumulate the specular contributions
    if (NdotL > 0.0) {
        //use lightcolor for specular ...
        fragmentcolor += gl_FrontLightProduct[0].specular * pow(max(0.0, dot(N, NH)), specularexponent);
        //ignore lightcolor and use white lightcolor for specular ...
        //fragmentcolor.rgb += pow(max(0.0, dot(N, NH)), specularexponent);
    }
    fragmentcolor *= shadow;
    gl_FragColor = fragmentcolor;

    //gl_FragColor = vec4( N.xyz *.5 + vec3(.5,.5,.5) ,1 );        
}
