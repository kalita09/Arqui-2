/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyecto;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author b04732
 */

public class Nucleo implements Runnable {
        //Contexto
	int PC;
        int[] registros;
        int numInstruccion;
        
        String IR;
	Bloque[] cacheInstrucciones;
	
	int BLOQUES;
	int apuntadorCache;
	String nombreNucleo;
        private CyclicBarrier barrier;
        int pruebaHilo;
	Memoria memoria;
        int bloqueInicio;
        int bloqueFin;
        int posFin;
        boolean seguir;
        boolean terminado;
        static int quantum;
        static int ciclosReloj;
        static Semaphore busInstrucciones;
        int pcFin;
        int cicloBusCache;
        int latenciaMem;

        boolean falloCache;
        int contCiclosFallo;
        
	public Nucleo(String nombre, CyclicBarrier barrier,Memoria memoria,int bloqueInicio,int pcFin,int quantum,int ciclosReloj,int cicloBus, int latenciaMem,Semaphore busInstrucciones) {

		this.nombreNucleo = nombre;
                this.barrier = barrier;
		this.registros = new int[32];
		this.BLOQUES = 8;
		this.apuntadorCache = 0;
		this.cacheInstrucciones = new Bloque[BLOQUES];
		this.pruebaHilo = 1;
                this.memoria = memoria;
                this.inicializarCaches();
                this.bloqueInicio = bloqueInicio;
                this.pcFin = pcFin;
                this.seguir = true;
                this.terminado = false;
                Nucleo.quantum = quantum;
                Nucleo.ciclosReloj = ciclosReloj;
                this.busInstrucciones = busInstrucciones;
                this.falloCache = false;
                this.latenciaMem = latenciaMem;
                this.cicloBusCache = (cicloBus+this.latenciaMem+cicloBus)*4;
                this.contCiclosFallo = this.cicloBusCache;
	}
	
	private void inicializarCaches() {
		for(int i=0; i<BLOQUES; i++) {
			cacheInstrucciones[i] = new Bloque(-1); //-1 para distinguir estos bloques "vacios"
			cacheInstrucciones[i].inicializarCache();
		}
	}
	
	public void cargarBloque(Bloque b) {
		cacheInstrucciones[b.ID%this.BLOQUES] = b;
                
	}
        void setPC(int miPC){
            this.PC = miPC;
        }
        int getPC(){
            return this.PC;
        } 
        //El nucleo termino el hilo anterior ahora es el length del nuevo hilo
        void setPCFin(int mipcFin){
            this.pcFin = mipcFin;
        }
	
	public void imprimirCache(){
            for(int bloque = 0; bloque < 8; bloque++ ){
                System.out.print("BLoque "+bloque +" ");
                this.cacheInstrucciones[bloque].imprimir();

            }
        }
        public void imprimirRegistros(){
            for(int registro = 0; registro < 32; registro++ ){
                System.out.print("Registro "+registro +" "+this.registros[registro]);
                

            }
            System.out.println("H");
        }
	
	public boolean contenerBloque() {
		for(int i=0; i<BLOQUES; i++) {
                   // (this.PC/4)%this.BLOQUES
			if(cacheInstrucciones[i].getID() == this.bloqueInicio+this.PC/4) { // PC/4 nos da el numero de bloque
				return true;
			}
		}
		return false;
	}
	
	public void setPrueba(int num){
        this.pruebaHilo = num;
        
        }
        public void setContexto(int nuevoPC,int[] nuevoRegistros){
            this.PC = nuevoPC;
            this.registros = nuevoRegistros;
        }

    @Override
    public void run() {
        /*
        while(seguir){
        System.out.println(this.nombreNucleo);
            try {
            //fallo de cache nucleo 1 (falta el bus)
            if(!this.contenerBloque()) {   
                System.out.print("hay fallo");
	            //esto debe ir en un ciclo hasta q se acaben los ciclos
            	Bloque b1 = this.memoria.getBloque(this.bloqueInicio+this.PC/4);
            	this.cargarBloque(b1);
                this.ejecutarInstruccion();
                this.barrier.await();
            }else{
                this.ejecutarInstruccion();
                this.barrier.await();
            
            }
            
            
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }*/

        while(seguir){
        System.out.println(this.nombreNucleo);
            try {
            System.out.print("pc"+this.PC+"bloque inicio "+this.bloqueInicio);
            //fallo de cache nucleo 1 (falta el bus)

            if(this.contenerBloque() && !this.falloCache) {
                this.ejecutarInstruccion();
                if(this.PC > this.pcFin){
                    this.terminado = true;
                }
            } else if(this.falloCache) {
            	contCiclosFallo--;
            	if(contCiclosFallo<=1) {
            		falloCache = false;
            		contCiclosFallo = this.cicloBusCache;
            		busInstrucciones.release();
            	}
            } else {


                if(busInstrucciones.tryAcquire()){
                   
                	Bloque b1 = this.memoria.getBloque(this.bloqueInicio+this.PC/4);
                	cargarBloque(b1);
                	System.out.print("cargando"+this.bloqueInicio+this.PC/4+".");
                	System.out.print("hay fallo");
                	falloCache = true;
                    contCiclosFallo--;
                                	cargarBloque(b1);
            	System.out.print("cargando"+this.bloqueInicio+this.PC/4+".");
            	System.out.print("hay fallo");   
                }
            }
            
            try {
            	this.barrier.await();
            } catch(BrokenBarrierException ex) {}

            
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        

    }
	
	public void ejecutarInstruccion() {
            //Bloque donde se encuentra la instruccion apuntada por el PC actual, previamente cargada

		Bloque b = cacheInstrucciones[((this.PC/4)%this.BLOQUES)+this.bloqueInicio  ];

		//Bloque b = cacheInstrucciones[numInstruccion/4];

            //Intruccion del bloque (0|1|2|3) 
                
             //Pido instruccion al cache la guardo en el IR  
		IR = b.getInstruccion(this.PC%4);
                System.out.println(IR);
		String[] codificacion = IR.split(" ");
		System.out.println(codificacion[0]);
                
            //Verifica cual operacion es
		switch(codificacion[0]) {
			case "8": //DADDI
				registros[Integer.parseInt(codificacion[2])] =
					Integer.parseInt(codificacion[1]) + Integer.parseInt(codificacion[3]);
			break;
			
			case "32": //DADD
				registros[Integer.parseInt(codificacion[3])] =
					Integer.parseInt(codificacion[1]) + Integer.parseInt(codificacion[2]);
			break;
			
			case "34": //DSUB
				registros[Integer.parseInt(codificacion[3])] =
					Integer.parseInt(codificacion[1]) - Integer.parseInt(codificacion[2]);
			break;
			
			case "12": //DMUL
				registros[Integer.parseInt(codificacion[3])] =
					Integer.parseInt(codificacion[1]) * Integer.parseInt(codificacion[2]);
			break;
			
			case "14": //DDIV
				registros[Integer.parseInt(codificacion[3])] =
					Integer.parseInt(codificacion[1]) / Integer.parseInt(codificacion[2]);
			break;
			
			case "4": //BEQZ
				if(Integer.parseInt(codificacion[1]) == 0) {
					this.PC += Integer.parseInt(codificacion[3]); //multiplicado*4???????????????
				}
			break;
			
			case "5": //BNEZ
				if(Integer.parseInt(codificacion[1]) != 0) {
					this.PC += Integer.parseInt(codificacion[3]); //multiplicado*4???????????????
				}
			break;
			
			case "3": //JAL
				registros[31] = this.PC;
				this.PC += Integer.parseInt(codificacion[3]);
			break;
			
			case "2": //JR
				this.PC = Integer.parseInt(codificacion[1]);
			break;
		}
		this.PC++;
	}
	
}

