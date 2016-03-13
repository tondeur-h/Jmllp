/*
 * Copyright (C) 2016 tondeur-h
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jmllp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author tondeur-h
 */
public class Jmllp {

    ServerSocket servSock;
    Socket sockIn;


    int port=1234;
    String propFileName="Jmllp.properties";
    String destinationPath;
    String socketName="mllp1";
    String extensionName="txt";
    long autoCounter=0;
    boolean ack=false;
    boolean mllp=false;
    boolean log=false;
   // String ackOUT="";



    /***************
     * Constructeur
     ***************/
    public Jmllp() {
        try {

            //lire le fichier paramétrage
            lire_properties();

            servSock=new ServerSocket(port);
            show_running();

            while (true){
            //attendre une connexion socket
                sockIn=servSock.accept();
                autoCounter++;
                ChannelX chx=new ChannelX(sockIn,autoCounter,destinationPath,socketName,extensionName,ack,mllp,log);
            }

        } catch (IOException ex) {
            Logger.getLogger(Jmllp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }




    /*****************************************
     * @param args the command line arguments
     *****************************************/
    public static void main(String[] args) {
        new Jmllp();
    }


    /********************************
     * lire le fichier des propriétés
     ********************************/
    private void lire_properties() {
      String pathApplication=System.getProperty("user.dir");
      Properties p=new Properties();
      FileInputStream inProp;

        try {
            //ouvrir le fichier
            inProp=new FileInputStream(pathApplication+"/"+propFileName);
            //lire les variables
            p.load(inProp);
            port=Integer.parseInt(p.getProperty("port", "1234"), 10);
            socketName=p.getProperty("socketName", "mllp1");
            destinationPath=p.getProperty("destinationPath", pathApplication);
            extensionName=p.getProperty("extensionName", "txt");
            autoCounter=Long.parseLong(p.getProperty("autoCounter","1"), 10);
            ack=Boolean.valueOf(p.getProperty("ack", "false"));
            mllp=Boolean.valueOf(p.getProperty("mllp", "false"));
            log=Boolean.valueOf(p.getProperty("log", "false"));
            inProp.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Jmllp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Jmllp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }


/**
 * Affichage des information du connecteur
 */
    private void show_running() {
        System.out.println("Jmllp Running");
        System.out.println("On port : "+port);
        System.out.println("Destination Path : "+destinationPath);
        System.out.println("ack ? : "+ack);
        System.out.println("mllp ? : "+mllp);
        System.out.println("log ? : "+log);
        System.out.println("Begin Counter : "+autoCounter);
        System.out.println("Socket prefix Name : "+socketName);
        System.out.println("..................................");
    }
}




class ChannelX extends Thread{

    DataInputStream binstr;
    DataOutputStream boutstr;
    byte[] bufferInput;
    byte[] bufferOutput;
    Socket sockIn;
    long autoCounter;
    String socketName;
    String pathName;
    String extensionName;
    boolean ack;
    boolean mllp;
    boolean log;
    int bufferSizeIn,bufferSizeOut;


    public ChannelX(Socket sockIn,long autoCount,String pathName,String sockName, String extensionName,boolean ack,boolean mllp, boolean log) {
        //dimensionner les buffers
            bufferInput=new byte[1024*4096];
            this.sockIn=sockIn;
            autoCounter=autoCount;
            this.socketName=sockName;
            this.pathName=pathName;
            this.extensionName=extensionName;
            this.ack=ack;
            this.mllp=mllp;
            this.log=log;

            this.start();
    }




    @Override
    public void run(){
        try{
                //gerer la connexion
                if (log) System.out.println("Connection ON");
                //recuperer les flux IO
                binstr=new DataInputStream(sockIn.getInputStream());
                boutstr=new DataOutputStream(sockIn.getOutputStream());

                bufferSizeIn=binstr.available();
                //lire les données en entrée
                if (binstr.available()>0){
                    //lire et traiter le buffer d'entrée
                    binstr.read(bufferInput);
                   if (log) System.out.println("Read "+bufferSizeIn+" bytes");

                    bufferOutput=traiter_buffer_in(bufferInput,bufferSizeIn);
                }

                //renvoyer un ACK vers le port de sortie.
                if (ack) {boutstr.write(mllp_encapsulate(bufferOutput,bufferSizeOut),0,bufferSizeOut+3);}

                binstr.close();
                boutstr.close();

                    //fermer la socket
                sockIn.close();

        }catch(IOException ioe){
            ioe.printStackTrace();
            }
        }



    /**************************************************************
     * Traiter les données du buffer pour les écrire sur le disque
     * et préparer l'ACK de retour
     * @param bufferInput
     * @return
     **************************************************************/
    private byte[] traiter_buffer_in(byte[] bufferIn,int bufSizeIn) {
        PrintWriter pwDisk=null;
        try {
            //mllp de_Encapsulate
            //ByteBuffer currBuf=mllp_unencapsulate(bufferIn);
            //write on disk
            String fileName=pathName+"/"+socketName+autoCounter+"."+extensionName;
            if (log) System.out.println("Write fileName : "+fileName);
            pwDisk = new PrintWriter(fileName);
            String BufInValue;
            if (mllp==true) {
            if (log) System.out.println("mllp decode");
            BufInValue=new String(mllp_UnEncapsulate(bufferIn,bufSizeIn),0,bufSizeIn-3);
            if (log) System.out.println("Write "+(bufSizeIn-3)+" bytes");
            }
            else
            {
                BufInValue=new String(bufferIn,0,bufSizeIn);
               if (log) System.out.println("Write "+bufSizeIn+" bytes");
            }
            pwDisk.print(BufInValue);
            pwDisk.close();
            autoCounter++;
            //return ACK
            bufferOutput=new byte[1024*4096];
            //TODO : construire ACK
            String strAck="MSH|^~\\&|JMLLP|CHV|"+socketName+"|CHV|20061019172719||ACK^O01|"+socketName+autoCounter+"|P|2.3\nMSA|AA|"+socketName+autoCounter;

            bufferSizeOut=strAck.length();
            bufferOutput=mllp_encapsulate(strAck.getBytes(),bufferSizeOut);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Jmllp.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            pwDisk.close();
        }
        return bufferOutput;
    }



    /***************************************************************
     * Ajouter les byte d'encapsulation mllp au données du buffer...
     * The header is a vertical tab character <VT> its hex value is 0x0b.
     * The trailer is a file separator character <FS> (hex 0x1c) immediately
     * followed by a carriage return <CR> (hex 0x0d)
     * @param bufferOutput
     * @return
     ***************************************************************/
    private byte[] mllp_encapsulate(byte[] bufferLocIn,int sizebufLocIn) {
        //TODO :mllp encapsulate a tester
        byte[] out=new byte[sizebufLocIn+3];
        int idx;
        out[0]=0x0b;
        for (idx=1;idx<sizebufLocIn;idx++){
            out[idx]=bufferLocIn[idx-1];
        }
        out[idx+1]=0x1c;
        out[idx+2]=0x0d;
        bufferSizeIn=sizebufLocIn+3;
        return out;
    }



    /***************************************************************
     * Retirer les byte d'encapsulation mllp au données du buffer...
     * @param bufferOutput
     * @return
     ***************************************************************/
    private byte[] mllp_UnEncapsulate(byte[] bufferLocIn,int sizebufLocIn) {
        byte[] out=new byte[sizebufLocIn-3];
        int idx=0;
        for (idx=0;idx<sizebufLocIn-3;idx++){
            out[idx]=bufferLocIn[idx+1];
        }
        bufferSizeIn=sizebufLocIn-3;
        return out;
    }



}