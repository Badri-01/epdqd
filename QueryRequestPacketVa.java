/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epdqd;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author hp
 */
public interface QueryRequestPacketVa extends Serializable{
    BigInteger getVa();
    BigInteger getCa_1();
    BigInteger getCa_2();
    List<BigInteger> getPrimes();
    BigInteger getTS();
    BigInteger getMACa();
}
