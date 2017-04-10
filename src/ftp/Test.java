package ftp;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by almul0 on 4/5/17.
 */
public class Test {

    static private String ftpPath = "FTP_ROOT/";

    public static void main(String[] args) {

        String fName = ftpPath + "renew2.5base.zip";
        File f = new File(fName);//obtenemos fichero

        System.out.println(f.getName());

        //Matcher expIn = Pattern.compile("(.*)\\.part\\d+-\\d+$").matcher(f.getName());

        Matcher expIn = Pattern.compile("(.*)\\.part(\\d+)-(\\d+)$").matcher(f.getName());

        if (expIn.find()) {
            System.out.println(expIn.group(1));
            System.out.println(expIn.group(2));
            System.out.println(expIn.group(3));
        } else {
            System.out.println("No group");
        }
    }
}
