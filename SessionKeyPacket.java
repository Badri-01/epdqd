/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import java.io.Serializable;

/**
 *
 * @author hp
 */
public interface SessionKeyPacket extends Serializable{
    String typeOfPacket();
    byte[] getPrivateKey();
    int getPort();
}
