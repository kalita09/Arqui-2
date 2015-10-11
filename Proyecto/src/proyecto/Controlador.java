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
            colaEspera = new int[3][tamanoCola];
            this.vectorContextos = new Contexto [tamanoCola];
            this.vectorNucleos = new Nucleo [tamanoCola];
            numeroHilos = tamanoCola;
            apuntadorCola = 0;
            apuntadorCola2 = 1;
            hiloActual1 = 1;
            hiloActual2 = 2;
            this.quantum = quantum;
            this.ciclosReloj = ciclosReloj;
            this.busInstrucciones = new Semaphore(1);
	}
	
        void iniciar(){
            this.m = new Memoria();
            
            int bloque;
            
            //Guarda en cola donde inicia cada hilo
            for(int j = 1; j <= numeroHilos; j++ ){   
                bloque = m.leerArchivo(j);
                colaEspera[0][j-1]=bloque;
                colaEspera[1][j-1]=m.getPosicion();

                System.out.println("Hilo "+j+" comienza en "+bloque+" "+m.getPosicion());
                
                
            }
            /*
            for(int j = 1; j <= numeroHilos; j++ ){ 
                int length = 0;
                //Tamano maximo PC
                //bloque hilo siguiente
                length += colaEspera[0][j]*16;
                //bloque hilo siguiente
                length += colaEspera[1][j]*4;
                colaEspera[2][j-1]=length;
            
            }*/
            for(int j = 0; j < numeroHilos; j++ ){   

                colaEspera[1][j]=m.getPosicion();

                System.out.println("Bloque "+ colaEspera[0][j]+" posicion "+ colaEspera[0][j]+" tamano "+ colaEspera[0][j]);
                
                
            }
            this.barrera = new CyclicBarrier(numeroHilos,this);
            
            //iniciar vector de nucleos
            for(int i=0; i<numNUCLEOS; i++) {
               
                vectorNucleos[i] = new Nucleo("Nucleo "+i,barrera,this.m,colaEspera[0][i],this.quantum, this.busInstrucciones);
                
                    
            }
            
            
    
            //inicializo vector de contextos
            for(int i=0; i<numeroHilos; i++) {
                    vectorContextos[i] = new Contexto();
            }
                    
            
            
            
            //aqui cargar contexto
            
            for(int i=0; i<numNUCLEOS; i++) {
                    vectorNucleos[i].setContexto(this.vectorContextos[i].PC,this.vectorContextos[i].registros);
            }

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
        Nucleo.quantum--;
        System.out.println("Quantum"+Nucleo.quantum);
        
        
        System.out.println("Despues registro");
            for(int i=0; i<numeroHilos; i++) {
                    System.out.println("Nucleo "+i);
                    vectorNucleos[i].imprimirRegistros();
            }
            
            this.m.imprimirMem();
        System.out.println("Despues cache");
            for(int i=0; i<numeroHilos; i++){ 
                    System.out.println("Nucleo "+i);
                    vectorNucleos[i].imprimirCache();
            }
     
        
        

        if(Nucleo.quantum == 0){
            
            for(int i=0; i<numeroHilos; i++){ 
                System.out.println("Cache "+i);
                vectorNucleos[i].seguir=false;
            }
           //guardo vector de contextos
          
            for(int i=0; i<numeroHilos; i++) {
                    vectorContextos[i].guardarContexto(this.vectorNucleos[i].PC,this.vectorNucleos[i].registros );
            }
            
            
          
          
        }


     }
}
