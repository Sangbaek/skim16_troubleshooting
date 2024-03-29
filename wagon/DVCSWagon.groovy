package wagon;

import org.jlab.clas.physics.LorentzVector
import org.jlab.clas.physics.Vector3
import org.jlab.clas.pdg.PDGDatabase
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;

/**
 * 
 * DVCS Skimming
 *
 * @author fxgirod
 */

class DVCSWagon {

	def targetMass = PDGDatabase.getParticleMass(2212)
	def beamEnergy = 10.604

	def Vangle(Vector3 v1, Vector3 v2){
		def res=0;
		def l1 = v1.mag();
		def l2 = v2.mag();
		if( l1*l2 > 0)res = Math.toDegrees( Math.acos( v1.dot(v2)/(l1*l2) ) );
		return res;
	}

	 def processDataEvent(Event event, SchemaFactory factory) {
		LorentzVector VB = new LorentzVector(0,0,beamEnergy,beamEnergy);
		LorentzVector VT = new LorentzVector(0,0,0,targetMass);

		Bank RecPart = new Bank(factory.getSchema("REC::Particle"));
		event.read(RecPart);

		boolean hasDVCS = false;
		if(RecPart.getRows()>2 ){
			int n_e = 0;
			int n_p = 0;
			int n_g = 0;
			for (int ii = 0; ii < RecPart.getRows() ; ii++) {
				int is_pid = RecPart.getInt("pid", ii);
				int stat   = Math.abs(RecPart.getShort("status", ii));
				if(stat>2000  && stat<4000 && is_pid==11)n_e++;
				if(stat>2000  && stat!=4000 && is_pid==2212)n_p++;
				if(stat!=2000 && stat<4000 && is_pid==22)n_g++;
			}
			boolean is_candidate = n_e*n_p*n_g>0;
			if(is_candidate){
				int[] e_ind   = new int[n_e];
				int[] p_ind   = new int[n_p];
				int[] g_ind   = new int[n_g];
				n_e = 0;
				n_p = 0;
				n_g = 0;
				for (int ii = 0; ii < RecPart.getRows() ; ii++) {
					int is_pid = RecPart.getInt("pid", ii);
					int stat   = Math.abs(RecPart.getShort("status", ii));
					if(stat>2000  && stat<4000 && is_pid==11){e_ind[n_e]=ii;n_e++;}
					if(stat>2000  && stat!=4000 && is_pid==2212){p_ind[n_p]=ii;n_p++;}
					if(stat!=2000 && stat<4000 && is_pid==22){g_ind[n_g]=ii;n_g++;}
				}
				for (int ie = 0; ie < n_e && !hasDVCS; ie++) {
					def e_px  = RecPart.getFloat("px", e_ind[ie]);
					def e_py  = RecPart.getFloat("py", e_ind[ie]);
					def e_pz  = RecPart.getFloat("pz", e_ind[ie]);

					def e_mom = Math.sqrt(e_px*e_px+e_py*e_py+e_pz*e_pz);
					if( e_mom>0.1*beamEnergy ){
						LorentzVector VE = new LorentzVector(e_px,e_py,e_pz,e_mom);
						for (int ip = 0; ip < n_p && !hasDVCS; ip++) {
							def p_px  = RecPart.getFloat("px", p_ind[ip]);
							def p_py  = RecPart.getFloat("py", p_ind[ip]);
							def p_pz  = RecPart.getFloat("pz", p_ind[ip]);

							def p_ene = Math.sqrt(p_px*p_px+p_py*p_py+p_pz*p_pz+targetMass*targetMass);
							if( p_ene>0.94358 ){
								LorentzVector VP = new LorentzVector(p_px,p_py,p_pz,p_ene);
								for (int ig = 0; ig < n_g && !hasDVCS; ig++) {
									def g_px  = RecPart.getFloat("px", g_ind[ig]);
									def g_py  = RecPart.getFloat("py", g_ind[ig]);
									def g_pz  = RecPart.getFloat("pz", g_ind[ig]);

									def g_mom = Math.sqrt(g_px*g_px+g_py*g_py+g_pz*g_pz);
									if(  g_mom>0.15*beamEnergy){
										LorentzVector VG = new LorentzVector(g_px,g_py,g_pz,g_mom);

										LorentzVector Q = new LorentzVector(0,0,0,0);
										Q.add(VB);
										Q.sub(VE);
										LorentzVector W = new LorentzVector(0,0,0,0);
										W.add(Q);
										W.add(VT);

										if( -Q.mass2()>0.8 && W.mass()>1.8 ){
											LorentzVector VmissP = new LorentzVector(0,0,0,0);
											VmissP.add(W);
											VmissP.sub(VG);
											LorentzVector VmissG = new LorentzVector(0,0,0,0);
											VmissG.add(W);
											VmissG.sub(VP);
											LorentzVector VmissAll = new LorentzVector(0,0,0,0);
											VmissAll.add(VmissG);
											VmissAll.sub(VG);

											hasDVCS = (true
												&& VmissAll.e() > -1 && VmissAll.e() < 2.0
												&& VmissP.mass() > 0.25 && VmissP.mass() < 2.0
												&& VmissAll.mass2() > -0.1 &&  VmissAll.mass2() < 0.1
												&& VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py() < 0.75
												&& Vangle( VG.vect() , VmissG.vect() ) < 7.5)
												;
											if (hasDVCS) {
												println("e_mom, p_ene, g_mom, Q2, W, ME, MM_eg, MM_epg, MPt^2, theta_gg")
												print(e_mom+"   ")
												print(p_ene+"   ")
												print(g_mom+"   ")
												print(-Q.mass2()+"   ")
												print(W.mass()+"   ")
												print(VmissAll.e()+"   ")
												print(VmissP.mass()+"   ")
												print(VmissAll.mass2()+"   ")
												print(VmissAll.px()*VmissAll.px() + VmissAll.py()*VmissAll.py()+"   ")
												println(Vangle( VG.vect(), VmissG.vect())+"   ")
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return hasDVCS;
	}
}
