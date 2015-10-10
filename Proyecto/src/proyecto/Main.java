/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyecto;

import java.util.concurrent.CyclicBarrier;

/**
 *
 * @author b04732
 */
public class Main {
    public static void main(String[] args) throws Exception {
        
        Controlador controlador = new Controlador(2,5);
        controlador.iniciar();
  

    }
}
