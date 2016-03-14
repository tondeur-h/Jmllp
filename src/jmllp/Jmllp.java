/*
 * Copyright (C) 2016 Tondeur-Hervé
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
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/********************
 * Classe principale Jmllp
 * @author Tondeur-hervé
 ********************/
public class Jmllp {

    ServerSocket servSock;
    Socket sockIn;


    int port=1234;
    String propFileName="jmllp.properties"; //nom du fichier parametre du connecteur
    String destinationPath;
    String socketName="mllp1";
    String extensionName="txt";
    long autoCounter=0;
    boolean ack=false;
    boolean mllp=false;
    boolean log=false;


    /*********************************
     * Constructeur
     * Boucle sur les connexions socket
     * creer un objet ChannelX pour
     * chaque socket entrante
     *********************************/
    public Jmllp() {
        try {

            //lire le fichier paramétrage
            //et appliquer les paramétres du fichier
            //jmllp.properties
            lire_properties();

            //instance du serveur socket
            servSock=new ServerSocket(port);
            //afficher les infos de demarrage de la socket
            show_running();

            //boucle de connexion des sockets
            while (true){
            //attendre une connexion socket
                sockIn=servSock.accept();
                //augmenter le numéro du fichier
                autoCounter++;
                //instance de prise en charge d'une socket
                ChannelX chx=new ChannelX(sockIn,autoCounter,destinationPath,socketName,extensionName,ack,mllp,log);
            }

        } catch (IOException ex) {
            Logger.getLogger(Jmllp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /*****************************************
     * @param args the command line arguments
     * Appel le constructeur de l'application
     * pour démarrer la socket Serveur
     *****************************************/
    public static void main(String[] args) {
        new Jmllp();
    }


    /****************************************
     * lire le fichier des propriétés
     * le fichier properties doit se trouver
     * dans le dossier de travail de l'appli
     * et doit se nommer jmllp.properties
     * les paramétres de ce fichiers sont
     * port = numero de port de la socket serveur
     * socketName = nom de la socket et prefix du nom des fichiers de sorties
     * destinationPath = chemin de destination des fichiers (local ou URI)
     * extensionName = nom de l'extension des fichiers destination
     * autoCounter = numero de debut de numerotation des fichiers destination
     * ack = boolean autorisant le retour d'un ACK vers l'émetteur
     * mllp = boolean autirisant l'encapsulation des messages entrant en mllpV1
     * log = boolean autorisant l'affichage des traces.
     ****************************************/
    private void lire_properties() {
        //récuperer le dossier de travail de l'application
      String pathApplication=System.getProperty("user.dir");
      //creer l'objet properties permettant de relire le fichier properties
      Properties p=new Properties();
      FileInputStream inProp;

        try {
            //ouvrir le fichier
            //le fichier doit se nommer jmllp.properties et se trouver
            //obligatoirement dans le dossier de l'application
            inProp=new FileInputStream(pathApplication+"/"+propFileName);
            //lire les variables du fichier
            p.load(inProp);
            //numero port d'écoute 1234 par défaut
            port=Integer.parseInt(p.getProperty("port", "1234"), 10);
            //nom de la socket, donne ce nom aux fichiers en sortie
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


/*****************************************
 * Affichage des informations du connecteur
 * Numero de version / Copyright / Licence
 ******************************************/
    private void show_running() {
        System.out.println("Jmllp version 1.0");
        System.out.println("Copyright (C) 2016 Tondeur Hervé");
        System.out.println("-----------------------------------------------------------------------");
         System.out.println(
         "This program is free software: you can redistribute it and/or modify\n"+
         "it under the terms of the GNU General Public License as published by\n"+
         " the Free Software Foundation, either version 3 of the License, or\n"+
         "(at your option) any later version.\n"+
         "This program is distributed in the hope that it will be useful,\n"+
         "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"+
         "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"+
         "GNU General Public License for more details.\n"+
         "You should have received a copy of the GNU General Public License\n"+
         "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n"+
         "-----------------------------------------------------------------------");
        System.out.println("Jmllp Running on port : "+port);
        System.out.println("Destination Path : "+destinationPath);
        System.out.println("ack : "+ack);
        System.out.println("mllp : "+mllp);
        System.out.println("log : "+log);
        System.out.println("Beginning Counter : "+autoCounter);
        System.out.println("Destination files path : "+destinationPath);
        System.out.println("Socket prefix Name : "+socketName);
        System.out.println("File extension : "+extensionName);
        System.out.println("..................................");
    }
} //fin de la Class Jmllp

//==========================================================================================

/*************************
 * Classe ChannelX (decode un message socket vers fichier disque)
 * @author Tondeur-Hervé
 * @version Copyright 2016
 * @see Licence GPL V3
 *************************/
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


/*****************************************
 * Constructeur du gestionnaire du channel
 * @param sockIn
 * @param autoCount
 * @param pathName
 * @param sockName
 * @param extensionName
 * @param ack
 * @param mllp
 * @param log
 ******************************************/
    public ChannelX(Socket sockIn,long autoCount,String pathName,String sockName, String extensionName,boolean ack,boolean mllp, boolean log) {
        //dimensionner le buffer de lecture du message entrant
            bufferInput=new byte[1024*4096];
            //affectation des variables locales à partir
            //des valeurs transmises par la classe appelante
            this.sockIn=sockIn;
            this.autoCounter=autoCount;
            this.socketName=sockName;
            this.pathName=pathName;
            this.extensionName=extensionName;
            this.ack=ack;
            this.mllp=mllp;
            this.log=log;
            //exécuter la Thread (lancement de la méthode run)
            this.start();
    }



/***********************
 * méthode run du Thread
 * Surcharge de la méthode
 ***********************/
    @Override
    public void run(){
        try{
                //gerer la connexion
                if (log) System.out.println("Connection ON "+sockIn.getInetAddress().getHostAddress()+" "+sockIn.getPort());
                //recuperer les flux IO de la socket en cours
                binstr=new DataInputStream(sockIn.getInputStream());
                boutstr=new DataOutputStream(sockIn.getOutputStream());

                //calculer le nombre d'octets dans le messages
                bufferSizeIn=binstr.available();
                //lire les données en entrée s'il y en à
                if (binstr.available()>0){
                    //lire et traiter le buffer d'entrée
                    binstr.read(bufferInput);
                    //du log...
                   if (log) System.out.println("Read "+bufferSizeIn+" bytes");
                   //lancer le traitement d'écriture sur disque du message socket
                    bufferOutput=traiter_buffer_in(bufferInput,bufferSizeIn);
                }

                //renvoyer l'ACK vers le port de sortie si option demandée.
                if (ack) {boutstr.write(mllp_encapsulate(bufferOutput,bufferSizeOut),0,bufferSizeOut);}

                //fermer les flux IO
                binstr.close();
                boutstr.close();

                //fermer la socket
                sockIn.close();

        }catch(IOException ioe){
            ioe.printStackTrace();
            }
        }


    /**************************************************************
     * Traiter les données du buffer pour les écrires sur le disque
     * et préparer l'ACK de retour
     * @param bufferInput
     * @return
     **************************************************************/
    private byte[] traiter_buffer_in(byte[] bufferIn,int bufSizeIn) {
        //objet d'écriture sur disque
        PrintWriter pwDisk=null;
        try {
            //msgAckResponse variable qui va contenir le msh-10.1 a inserer dans le champ MSA-3.1 de l'ACK
            String msgAckResponse;

            //Construire le chemin destination du fichier sur le disque
            String fileName=pathName+"/"+socketName+autoCounter+"."+extensionName;
            //du log...
            if (log) System.out.println("Write fileName : "+fileName);
            //instance de l'objet ecriture sur disque
            pwDisk = new PrintWriter(fileName);

            //convertir le contenu du buffer provenant de la socket
            //en une chaine de caractéres
            String BufInValue;
            if (mllp==true) {
            if (log) System.out.println("mllp decode");
            //de-encapsule le mllp si option prévu et converti en chaine de caractéres
            BufInValue=new String(mllp_UnEncapsulate(bufferIn,bufSizeIn),0,bufSizeIn-3);
            if (log) System.out.println("Write "+(bufSizeIn-3)+" bytes");
            }
            else
            {
                // convertir le buffer en chaine de caractéres (option : sans mllp)
                BufInValue=new String(bufferIn,0,bufSizeIn);
               if (log) System.out.println("Write "+bufSizeIn+" bytes");
            }
            //extraire le champ MSH-10.1 du message d'origine
            msgAckResponse=extract_msgAckResponse(BufInValue);

            if (log) System.out.println("Message control Id : "+msgAckResponse);

            //ecrire le message contenu dans le buffer sur disque
            pwDisk.print(BufInValue);
            pwDisk.close();

            //autoCounter++;
            //Construire le buffer de sortie de l'ACK
            bufferOutput=new byte[1024*4096];
            //construire l'ACK et calculer sa taille
            Calendar c=Calendar.getInstance();
            //definir la date de maintenant...
            String dateMSG=""+c.get(Calendar.YEAR)+padZero((c.get(Calendar.MONTH)+1))+padZero(c.get(Calendar.DAY_OF_MONTH))+padZero(c.get(Calendar.HOUR_OF_DAY))+padZero(c.get(Calendar.MINUTE))+padZero(c.get(Calendar.SECOND));
            //construire le message ACK
            String strAck="MSH|^~\\&|JMLLP|CHV|"+socketName+"|CHV|"+dateMSG+"||ACK^O01|"+socketName+autoCounter+"|P|2.3\rMSA|AA|"+msgAckResponse;
            bufferSizeOut=strAck.length();

            //du log...
            if (log) System.out.println(strAck);
            if (log) System.out.println("ack response lenght "+bufferSizeOut+" bytes");

            //encapsuler dans le format mllp le message ACK et le placer dans un buffer de bytes
            bufferOutput=mllp_encapsulate(strAck.getBytes(),bufferSizeOut);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Jmllp.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            pwDisk.close();
        }
        //retourner le buffer contenant l'ACK
        return bufferOutput;
    }


    /***************************************************************
     * Ajouter les bytes d'encapsulation mllp aux données du buffer...
     * MLLP ?
     * The header is a vertical tab character <VT> its hex value is 0x0b.
     * The trailer is a file separator character <FS> (hex 0x1c) immediately
     * followed by a carriage return <CR> (hex 0x0d)
     * @param bufferOutput
     * @return
     ***************************************************************/
    private byte[] mllp_encapsulate(byte[] bufferLocIn,int sizebufLocIn) {
        //creer un buffer temporaire augmenté de 3 octets
        //pour inserer l'octet du header et les 2 octets du trailer
        byte[] out=new byte[sizebufLocIn+3];
        int idx;
        //inserer octet du header
        out[0]=0x0b;
        //recopier le buffer source dans le buffer destination
        //a la position 1 du tempon destination
        for (idx=1;idx<sizebufLocIn;idx++){
            out[idx]=bufferLocIn[idx];
        }
        //inserer les 2 octets du trailer en fin de buffer temporaire
        out[idx]=0x1c;
        out[idx+1]=0x0d;
        //ajuster la taille du nouveau tampon de sortie.
        bufferSizeOut=sizebufLocIn+3;
        //retourner le buffer temporaire
        return out;
    }


    /***************************************************************
     * Retirer les bytes d'encapsulation mllp aux données du buffer...
     * @param bufferOutput
     * @return
     ***************************************************************/
    private byte[] mllp_UnEncapsulate(byte[] bufferLocIn,int sizebufLocIn) {
        //creer un buffer temporaire de sizebufLocIn - 3 octets (mllp)
        byte[] out=new byte[sizebufLocIn-3];
        int idx=0;
        //recopier le buffer source dans le buffer temporaire
        //en retirant le premier octet et les 2 derniers octets
        for (idx=0;idx<sizebufLocIn-3;idx++){
            out[idx]=bufferLocIn[idx+1];
        }
        //ajuster la nouvelle taille du buffer
        bufferSizeIn=sizebufLocIn-3;
        //retourner le buffer temporaire
        return out;
    }


    /******************************************
     * Extraire le champ MSH-10.1 (message control Id)
     * Il doit être transmis en retour au message
     * de reponse ACK dans le segment MSA-3
     * @param BufInValue
     * @return
     ******************************************/
    private String extract_msgAckResponse(String BufInValue) {
        //parser le segment MSH sur le signe pipe '|'
        Scanner sc=new Scanner(BufInValue);
        sc.useDelimiter("\\|");
        //Lire les 9 premiers champs
        sc.next();sc.next();sc.next();
        sc.next();sc.next();sc.next();
        sc.next();sc.next();sc.next();
        //retourner le champ numero 10
        return sc.next();
    }


    /****************************
     * bourrer les valeurs
     * sur 2 digits avec un zéro
     * @param valIn
     * @return
     ****************************/
    private String padZero(int valIn){
        String out;
        out=String.valueOf(valIn);
        if (out.length()<2){
            out="0"+out;
        }
        return out;
    }

} //fin class ChannelX