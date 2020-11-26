/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

/**
 *
 * @author hp
 */
public class Simulation {

    public static void main(String args[]) {
        Thread ccm = new Thread(new CCM());
        ccm.start();
        Thread rsu = new Thread(new RSU(9000,9001));
        //rsu.start();
        Runnable Va = new Vehicle(1,9000,9001);
        Thread V1 = new Thread(Va);
        Runnable Vb = new Vehicle(2,9000,9001);
        Thread V2 = new Thread(Vb);
        V1.start();
        V2.start();
        
    }

}
