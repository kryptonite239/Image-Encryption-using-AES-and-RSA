import java.io.File;
import java.io.FileInputStream;
import java.io.*;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;



public class Server{
	private static SecretKeySpec secretkey;
	private static byte[] key;
    static Key serverpubKey;
	public static void setKey(String mykey){
        try{
        key=mykey.getBytes("UTF-8");
        MessageDigest sha=MessageDigest.getInstance("SHA-1");
        key=sha.digest(key);
        key=Arrays.copyOf(key,16);
        secretkey=new SecretKeySpec(key, "AES");
        }
        catch(NoSuchAlgorithmException e){  }
        catch(UnsupportedEncodingException e){}
    }
	public static byte[] encrypt(byte[] data ,String sec){
        try {
            setKey(sec);
            Cipher cipher=Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretkey);  
            return cipher.doFinal(data);

        } catch (Exception e) {
            
        }
        return null;
    }
    public static byte[] keyenc(String s) throws Exception{
       
        Cipher c=Cipher.getInstance("RSA");
        c.init(Cipher.ENCRYPT_MODE, serverpubKey);
        byte[] sm=s.getBytes(StandardCharsets.UTF_8);
        byte[] enc=c.doFinal(sm);
        return enc;

    }
    
    
	public static void main(String[] args)throws Exception {
        ServerSocket ss=new ServerSocket(9999);
        Socket soc=ss.accept();
        Scanner s=new Scanner(System.in);
        System.out.println("Connection Established");
        System.out.println("Waiting for public key from the client...");
        byte[] lenb = new byte[4];
        soc.getInputStream().read(lenb,0,4);
        ByteBuffer bb = ByteBuffer.wrap(lenb);
        int len = bb.getInt();
        byte[] servPubKeyBytes = new byte[len];
        soc.getInputStream().read(servPubKeyBytes);
        System.out.println("Public key recieved of length: "+servPubKeyBytes.length);
        X509EncodedKeySpec ks = new X509EncodedKeySpec(servPubKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        serverpubKey = kf.generatePublic(ks);
        System.out.println("Enter the file path of the image you want to encrypt (If in same folder enter the name of Image):");
        String img=s.nextLine();
        s.close();
		File f=new File(img);
		FileInputStream fis=new FileInputStream(f);
		byte[] data=new byte[fis.available()];
		fis.read(data);
        System.out.println("Encrypting image of size: "+data.length);
        String k="";
        Random r=new Random();
        for(int i=0;i<117;i++){
            k+=Integer.toString(r.nextInt(10));
        }
        byte[] aeskey=keyenc(k);
        System.out.println("Sending ecnrypted aes key...");
        ByteBuffer b=ByteBuffer.allocate(4);
        b.putInt(aeskey.length);
        soc.getOutputStream().write(b.array());
        soc.getOutputStream().write(aeskey);
        System.out.println("Encrypted aes key sent succesfully!");
		byte[] enc=encrypt(data,k);
        System.out.println("Sending encrypted image data of size "+enc.length+" ...");
        DataOutputStream dot=new DataOutputStream(soc.getOutputStream());
        dot.writeInt(enc.length);
        dot.write(enc);
        System.out.println("Encrypted image data is sent succesfully");
        System.out.println("Exiting");
        dot.close();
        fis.close();
        soc.close();
        ss.close();
        
		
		
	}
}