/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyecto;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

/**
 *
 * @author b04732
 */
public class Controlador implements Runnable{
    int[][] colaEspera;
    Contexto [] vectorContextos;
    int numeroHilos;
    int apuntadorCola;
    int apuntadorCola2;
    int hiloActual1;
    int hiloActual2;
    Memoria m;
    CyclicBarrier barrera;
    Nucleo nucleo1;
    Nucleo nucleo2;
    Nucleo [] vectorNucleos;
    int quantum;
    int ciclosReloj;
    static int numNUCLEOS = 2;

    static Semaphore busInstrucciones;

    
	public Controlador(int tamanoCola,int quantum) {
            colaEspera = new int[4][tamanoCola];
            this.vectorContextos = new Contexto [tamanoCola];
            this.vectorNucleos = new Nucleo [tamanoCola];
            numeroHilos = tamanoCola;
            apuntadorCola = 0;
            apuntadorCola2 = 1;
            hiloActual1 = 1;
            hiloActual2 = 2;
            this.quantum = quantum;
            this.ciclosReloj = 0;

            this.busInstrucciones = new Semaphore(1);

	}
	
        void iniciar(){
            this.m = new Memoria();
            
            int bloque;
            
            //Guarda en cola donde inicia cada hilo
            for(int j = 1; j <= numeroHilos; j++ ){   
                bloque = m.leerArchivo(j);
                colaEspera[0][j-1] = bloque;
                colaEspera[1][j-1] = m.getPosicion();
                //guardo el length total de instrucciones por hilo = PC final                
                colaEspera[2][j-1] = m.getLength();
                //n han termnado
                colaEspera[3][j-1] = 0;
                System.out.println("Hilo "+j+" comienza en "+bloque+" "+m.getPosicion()+" PC"+colaEspera[2][j-1]);
                m.setLength();
                
            }

            /*
            for(int j = 0; j < numeroHilos; j++ ){   

                colaEspera[1][j]=m.getPosicion();

                System.out.println("Bloque "+ colaEspera[0][j]+" posicion "+ colaEspera[1][j]+" tamano "+ colaEspera[2][j]);
                
                
            }*/
            this.barrera = new CyclicBarrier(2,this);
            
            //iniciar vector de nucleos


            vectorNucleos[0] = new Nucleo("Nucleo 1",barrera,this.m,colaEspera[0][this.apuntadorCola],colaEspera[2][this.apuntadorCola],this.quantum,this.ciclosReloj,this.busInstrucciones);
            vectorNucleos[1] = new Nucleo("Nucleo 2",barrera,this.m,colaEspera[0][this.apuntadorCola2],colaEspera[2][this.apuntadorCola2],this.quantum,this.ciclosReloj,this.busInstrucciones);


            
    
            //inicializo vector de contextos
            for(int i=0; i<numeroHilos; i++) {
                    vectorContextos[i] = new Contexto();
            }
                    

            //aqui cargar contexto

            vectorNucleos[0].setContexto(this.vectorContextos[this.apuntadorCola].PC,this.vectorContextos[this.apuntadorCola].registros);
            vectorNucleos[1].setContexto(this.vectorContextos[this.apuntadorCola2].PC,this.vectorContextos[this.apuntadorCola2].registros);
            vectorNucleos[0].numInstruccion = 0;
            vectorNucleos[1].numInstruccion = 0;

            System.out.println("Antes registro");
            for(int i=0; i<numNUCLEOS; i++) {
                    vectorNucleos[i].imprimirRegistros();
            }
            
            this.m.imprimirMem();
            System.out.println("Antes cache");
            for(int i=0; i<numNUCLEOS; i++) {
                    vectorNucleos[i].imprimirCache();
            }
            Thread hilo1 = new Thread(vectorNucleos[0]);
            Thread hilo2 = new Thread(vectorNucleos[1]);
            hilo1.start();
            hilo2.start();
            

            
            //aqui se mandan a ejecutar los hilos
            
            
            //Imprimo lo que esta en cola
            /*
            for(int j = 0; j < numeroHilos; j++ ){   
                System.out.print(colaEspera[0][j]);
                System.out.println(colaEspera[1][j]);
            } */

        }

    @Override
     public void run() {
         
        System.out.println("Todos han llegado a la barrera");
        
        //si no estan en fallo de cache los dos nucleos entraron en el 
        //primer caso del if donde ejecutan una instruccion, por lo tanto se resta quantum
        if(!vectorNucleos[0].falloCache && !vectorNucleos[1].falloCache){
            Nucleo.quantum--;
            Nucleo.ciclosReloj++;
        }else{
            Nucleo.ciclosReloj++;
        }
        
        System.out.println("Quantum"+Nucleo.quantum);
        System.out.println("Ciclo reloj"+Nucleo.ciclosReloj);
        System.out.println("Despues registro");
            for(int i=0; i<2; i++) {
                    System.out.println("Nucleo "+i);
                    vectorNucleos[i].imprimirRegistros();
            }
            
            this.m.imprimirMem();
        System.out.println("Despues cache");
            for(int i=0; i<2; i++){ 
                    System.out.println("Nucleo "+i);
                    vectorNucleos[i].imprimirCache();
            }
            
            
            if(Nucleo.quantum == 0){
                if(vectorNucleos[0].terminado==true){//nucleo 1 termino su hilo

                    //guardo contexto de hilo terminado
                    this.vectorContextos[this.apuntadorCola].guardarContexto( this.vectorNucleos[0].PC,this.vectorNucleos[0].registros);
                     colaEspera[3][this.apuntadorCola] = 1;

                    if((this.apuntadorCola+2)<= this.numeroHilos){// si hay hilos por asinar

                        if((this.apuntadorCola)==(this.numeroHilos-2)   ){
                           this.apuntadorCola=0; 

                        }else{
                           this.apuntadorCola+=2; 
                        }
                        //asignacion de nuevo hilo para el nucleo
                        vectorNucleos[0].terminado=false;
                        vectorNucleos[0].setContexto(this.vectorContextos[this.apuntadorCola].PC,this.vectorContextos[this.apuntadorCola].registros);
                        vectorNucleos[0].bloqueInicio = colaEspera[0][this.apuntadorCola];
                        vectorNucleos[0].setPCFin(colaEspera[2][this.apuntadorCola]);
                        vectorNucleos[0].numInstruccion = 0;

                    }else{
                        vectorNucleos[0].seguir=false;
                    
                    }

                }else{
                    
                    //guardo contexto de hilo terminado
                    this.vectorContextos[this.apuntadorCola].guardarContexto( this.vectorNucleos[0].PC,this.vectorNucleos[0].registros);
                    
                    if((this.apuntadorCola+2)<= this.numeroHilos){

                        if((this.apuntadorCola)==(this.numeroHilos-2)   ){
                           this.apuntadorCola=0; 

                        }else{
                           this.apuntadorCola+=2; 
                        }
                        //asignacion de nuevo hilo para el nucleo
                        vectorNucleos[0].terminado=false;
                        vectorNucleos[0].setContexto(this.vectorContextos[this.apuntadorCola].PC,this.vectorContextos[this.apuntadorCola].registros);
                        vectorNucleos[0].bloqueInicio = colaEspera[0][this.apuntadorCola];
                        vectorNucleos[0].setPCFin(colaEspera[2][this.apuntadorCola]);
                        vectorNucleos[0].numInstruccion = 0;

                    }else{
                        vectorNucleos[0].seguir=false;
                    
                    }

                }

                if(vectorNucleos[1].terminado==true){//nucleo 2 termino su hilo

                    //guardo contexto de hilo terminado
                    this.vectorContextos[this.apuntadorCola2].guardarContexto( this.vectorNucleos[1].PC,this.vectorNucleos[1].registros);
                     colaEspera[3][this.apuntadorCola2] = 1;

                    if((this.apuntadorCola2+2)<= this.numeroHilos){

                        if((this.apuntadorCola2)==(this.numeroHilos-1)   ){
                           this.apuntadorCola2=0; 

                        }else{
                           this.apuntadorCola2+=2; 
                        }
                        //asignacion de nuevo hilo para el nucleo
                        vectorNucleos[1].terminado=false;
                        vectorNucleos[1].setContexto(this.vectorContextos[this.apuntadorCola2].PC,this.vectorContextos[this.apuntadorCola2].registros);
                        vectorNucleos[1].bloqueInicio = colaEspera[0][this.apuntadorCola2];
                        vectorNucleos[1].setPCFin(colaEspera[2][this.apuntadorCola2]);
                        vectorNucleos[1].numInstruccion = 0;

                    }else{
                        vectorNucleos[1].seguir=false;
                    
                    }

                }else{
                    
                    //guardo contexto de hilo terminado
                    this.vectorContextos[this.apuntadorCola2].guardarContexto( this.vectorNucleos[1].PC,this.vectorNucleos[1].registros);
                    
                    if((this.apuntadorCola2+2)<= this.numeroHilos){

                        if((this.apuntadorCola2)==(this.numeroHilos-1)   ){
                           this.apuntadorCola2=0; 

                        }else{
                           this.apuntadorCola2+=2; 
                        }
                        //asignacion de nuevo hilo para el nucleo
                        vectorNucleos[1].terminado=false;
                        vectorNucleos[1].setContexto(this.vectorContextos[this.apuntadorCola2].PC,this.vectorContextos[this.apuntadorCola2].registros);
                        vectorNucleos[1].bloqueInicio = colaEspera[0][this.apuntadorCola2];
                        vectorNucleos[1].setPCFin(colaEspera[2][this.apuntadorCola2]);
                        vectorNucleos[1].numInstruccion = 0;

                    }else{
                        vectorNucleos[1].seguir=false;
                    
                    }

                }
                //Nucleo.quantum
                
                
                
                
            }else{//quantum no es cero
               if(vectorNucleos[0].terminado==true){//nucleo 1 termino su hilo

                    //guardo contexto de hilo terminado
                    this.vectorContextos[this.apuntadorCola].guardarContexto( this.vectorNucleos[0].PC,this.vectorNucleos[0].registros);
                     colaEspera[3][this.apuntadorCola] = 1;

                    if((this.apuntadorCola+2)<= this.numeroHilos){

                        if((this.apuntadorCola)==(this.numeroHilos-2)   ){
                           this.apuntadorCola=0; 

                        }else{
                           this.apuntadorCola+=2; 
                        }
                        //asignacion de nuevo hilo para el nucleo
                        vectorNucleos[0].terminado=false;
                        vectorNucleos[0].setContexto(this.vectorContextos[this.apuntadorCola].PC,this.vectorContextos[this.apuntadorCola].registros);
                        vectorNucleos[0].bloqueInicio = colaEspera[0][this.apuntadorCola];
                        vectorNucleos[0].setPCFin(colaEspera[2][this.apuntadorCola]);
                        vectorNucleos[0].numInstruccion = 0;

                    }else{
                        vectorNucleos[0].seguir=false;
                    
                    }

                }
               
               
               
                if(vectorNucleos[1].terminado==true){//nucleo 2 termino su hilo
                
                //guardo contexto de hilo terminado
                this.vectorContextos[this.apuntadorCola2].guardarContexto( this.vectorNucleos[1].PC,this.vectorNucleos[1].registros);
                 colaEspera[3][this.apuntadorCola2] = 1;
                
                    if((this.apuntadorCola2+2)<= this.numeroHilos){

                        if((this.apuntadorCola2)==(this.numeroHilos-1)   ){
                           this.apuntadorCola2=0; 

                        }else{
                           this.apuntadorCola2+=2; 
                        }
                        //asignacion de nuevo hilo para el nucleo
                        vectorNucleos[1].terminado=false;
                        vectorNucleos[1].setContexto(this.vectorContextos[this.apuntadorCola2].PC,this.vectorContextos[this.apuntadorCola2].registros);
                        vectorNucleos[1].bloqueInicio = colaEspera[0][this.apuntadorCola2];
                        vectorNucleos[1].setPCFin(colaEspera[2][this.apuntadorCola2]);
                        vectorNucleos[1].numInstruccion = 0;

                    }else{
                        vectorNucleos[1].seguir=false;
                    
                    }
   
                }

            } 
            
            
            
         
        }
        
     
}
