/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 * @author hp
 */
public interface QueryRequestPacketVi extends Serializable{
    BigInteger getVi();
    BigInteger getCi();
    BigInteger getTS();
    BigInteger getMACi();
}
