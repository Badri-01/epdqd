/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import static java.lang.Thread.sleep;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author hp
 */
public class Simulation {

    private static int noOfThreads = 100;

    public static void main(String args[]) {
        Thread ccm = new Thread(new CCM(9000));
        ccm.start();
        Thread rsu = new Thread(new RSU(9000));
        rsu.start();
        try {
            sleep(3000);
        } catch (InterruptedException e) {

        }
        /*Runnable Va = new Vehicle(1,9000);
        Thread V1 = new Thread(Va);
        Runnable Vb = new Vehicle(2,9000);
        Thread V2 = new Thread(Vb);
        Runnable Vc = new Vehicle(3,
        9000);
        Thread V3 = new Thread(Vc);
        V1.start();
        V2.start();
        V3.start();*/
        ExecutorService executor = Executors.newFixedThreadPool(noOfThreads);
        for (int i = 1; i <= 50; i++) {
            Runnable V = new Vehicle(i, 9000);
            executor.execute(V);
        }
        executor.shutdown();
    }

}
