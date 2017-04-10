package ftp;

import java.io.File;

import java.io.IOException;
import java.net.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Alberto Mur & Javier Antoran.
*/


public class FTPService {

    public static final int SIZEMAX = 255; // Maximum size of UDP datagram
    public static final int SERVERPORT = 5000; // default ftp.server port
    public static final int TIMEOUT = 1000; //timeout en ms
    public static final int CHUNKSIZE = 1024; //bytes por bloque
    public static final int MAXSERVERTHREADS = 20; //max concurrent tcp handlers


    static public enum Command {HELLO, LIST, GET, QUIT, ERROR};
    static public enum Response {WCOME, OK, PORT, SERVERROR, BYE, UNKNOWN};

    public static String stringFromDatagramPacket(DatagramPacket dgp) {
        return new String (dgp.getData (),dgp.getOffset(), dgp.getLength());
    }

    public static Command commandFromString(String textCommand){

        String detect = "";
        for (Command k : Command.values()){
            detect += (k + "|");
        }
        detect = detect.substring(0, detect.length() - 1);
        Matcher expIn = Pattern.compile(detect).matcher(textCommand);

        if (expIn.find()) {
            return Command.valueOf(expIn.group());
        } else {
            return Command.ERROR;
        }
    }

    public static Response responseFromString(String textResponse){

        String detect = "";
        for (Response k : Response.values()){
            detect += (k + "|");
        }
        detect = detect.substring(0, detect.length() - 1);
        Matcher expIn = Pattern.compile(detect).matcher(textResponse);

        if (expIn.find()) {
            return Response.valueOf(expIn.group());
        } else {
            return Response.UNKNOWN;
        }

    }

    public static String requestedFile(String textCommand) throws IOException{

        String detect = "GET\\s+(.*)$";
        Matcher expIn = Pattern.compile(detect).matcher(textCommand);

        if (expIn.find()) {
            return expIn.group(1);
        } else {
            throw new IOException("Missing argument FILE from GET command");
        }

    }

    public static int portFromResponse(String textResponse){

        String detect = "PORT\\s+(\\d+)$";
        Matcher expIn = Pattern.compile(detect).matcher(textResponse);

        if (expIn.find()) {
            return Integer.parseInt(expIn.group(1));
        } else {
            return 0; // Si error en puerto que hacer???
        }

    }

    public static void sendUDPmessage(DatagramSocket s, String text,
                                      InetAddress rHost, int rPort)
            throws IOException{

        SocketAddress isa = new InetSocketAddress(rHost, rPort);

        sendUDPmessage(s, text, isa);

    }

    public static void sendUDPmessage(DatagramSocket s, String text,
                                      SocketAddress sa) throws IOException{
        byte[] buff;

        buff = text.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(buff, buff.length, sa);
        s.send(sendPacket);

    }

    public static String getFilenameFromPart(String filename) {

        Matcher expIn = Pattern.compile("(.*)\\.part\\d+-\\d+$")
                               .matcher(filename);

        if (expIn.find()) {
            return expIn.group(1);
        } else {
            return null;
        }
    }

    public static int[] getIntervalFromPart(String filename) {

        Matcher expIn = Pattern.compile(".*\\.part(\\d+)-(\\d+)$")
                .matcher(filename);

        if (expIn.find()) {
            int[] intval =  {Integer.parseInt(expIn.group(1)),
                    Integer.parseInt(expIn.group(2))};
            return intval;
        } else {
            return null;
        }
    }


    public static long getNChunks(File f, int chunkSize) {

        long size = f.length();
        long chunks = (size % chunkSize == 0) ? (size / chunkSize)
                                              : (size / chunkSize + 1);

        return chunks;
    }

}