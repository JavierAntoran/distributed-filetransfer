package ftp;

import java.io.IOException;
import java.net.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by StFrancisco on 21/03/2017.
*/


public class FTPService {

    public static final int SIZEMAX = 255; // Maximum size of datagram UDP
    public static final int SERVERPORT = 5000; // default ftp.server port
    public static final int TIMEOUT = 1000; //suponemos que son ms

    static public enum Command {HELLO, LIST, GET, QUIT, ERROR};
    static public enum Response {WCOME, OK, PORT, SERVERROR, BYE, UNKNOWN};

    public static Command commandFromString(String textcommand){

        String detect = "";
        for (Command k : Command.values()){
            detect += (k + "|");
        }
        detect = detect.substring(0, detect.length() - 1);
        Matcher expIn = Pattern.compile(detect).matcher(textcommand);

        if (expIn.find()) {
            return Command.valueOf(expIn.group());
        } else {
            return Command.ERROR;
        }

        // TODO: extraer el elemento de tipo Command correspondiente al comando
        //		introducido como par�metro en modo texto ('textcommand')
    }

    public static Response responseFromString(String textresponse){

        String detect = "";
        for (Response k : Response.values()){
            detect += (k + "|");
        }
        detect = detect.substring(0, detect.length() - 1);
        Matcher expIn = Pattern.compile(detect).matcher(textresponse);

        if (expIn.find()) {
            return Response.valueOf(expIn.group());
        } else {
            return Response.UNKNOWN;
        }

        // TODO: extraer el elemento de tipo Response correspondiente a la respuesta
        //		introducida como par�metro en modo texto ('textresponse')
    }

    public static String requestedFile(String textcommand) {

        String detect = "GET\\s+(.*)";
        Matcher expIn = Pattern.compile(detect).matcher(textcommand);

        if (expIn.find()) {
            return expIn.group(1);
        } else {
            return "error getting filename";
        }

        // TODO: extraer el nombre del fichero a descargar a partir del comando
        //		correspondiente, tipo GET <file>
    }

    public static int portFromResponse(String textresponse){

        String detect = "PORT\\s+(\\d+)";
        Matcher expIn = Pattern.compile(detect).matcher(textresponse);

        if (expIn.find()) {
            return Integer.parseInt(expIn.group(1));
        } else {
            return 0; // Si error en puerto que hacer???
        }
        // TODO: extraer el puerto (dato tipo 'int') a partir del texto que lo indica
        //		en la respuesta correspondiente, tipo PORT <port>
    }

    public static void sendUDPmessage(DatagramSocket s, String text, InetAddress rHost, int rPort) throws IOException{

        byte[] buff;

        buff = text.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(buff, buff.length, rHost, rPort);
        s.send(sendPacket);

    }

}