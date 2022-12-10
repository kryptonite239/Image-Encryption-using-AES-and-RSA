import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;
import java.security.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Client {
	private static SecretKeySpec secretkey;
	private static byte[] key;
    public static PrivateKey privateKey;
    public static PublicKey pubkey;
	public static void setKey(String mykey){
        try{
        key=mykey.getBytes("UTF-8");
        MessageDigest sha=MessageDigest.getInstance("SHA-1");
        key=sha.digest(key);
        key=Arrays.copyOf(key,16);
        secretkey=new SecretKeySpec(key, "AES");
        }
        catch(NoSuchAlgorithmException e){  } 
        catch (UnsupportedEncodingException e) {  }
    }
	public static byte[] decrypt(byte[] str,String sec) {
		try {
            setKey(sec);
            Cipher cipher=Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretkey);
            return cipher.doFinal(str);           
        } catch (Exception e) {
            
        }
        return null;
	}
    public static byte[] keydec(byte[] s) throws Exception{
        
        Cipher c=Cipher.getInstance("RSA");
        c.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] enc=c.doFinal(s);
        return enc;

    }
    
	public static void main(String[] args) throws Exception {
        Socket ss;
        System.out.println("Enter the IP address of server to connect (Enter L if you wish to connect to localhost):");
        Scanner s=new Scanner(System.in);
        String ip=s.nextLine();
        if(ip.charAt(0)=='L'){
            ss=new Socket(InetAddress.getLocalHost(),9999);
        }
        else{
            ss=new Socket(InetAddress.getByName(ip),9999);
        }
        System.out.println("Generating public key...");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();
        pubkey = pair.getPublic();
        System.out.println("Public key generated of length: "+pubkey.getEncoded().length);
        System.out.println("Sending the public key to server....");
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(pubkey.getEncoded().length);
        ss.getOutputStream().write(bb.array());
        ss.getOutputStream().write(pubkey.getEncoded());
        System.out.println("Public key sent!");
        System.out.println("Getting the encrypted aes key...");
        byte[] lenb=new byte[4];
        ss.getInputStream().read(lenb, 0, 4);
        ByteBuffer b=ByteBuffer.wrap(lenb);
        int len=b.getInt();
        byte[] aeskey=new byte[len];
        ss.getInputStream().read(aeskey);
        System.out.println("Encrypted aes key recieved!");
        privateKey = pair.getPrivate();
        byte[] finalkey=keydec(aeskey);
        String nkey=new String(finalkey);
        System.out.println("Waiting for encrypted image data...");
        int length;
        DataInputStream din=new DataInputStream(ss.getInputStream());
        length=din.readInt();
        byte[] content=new byte[length];
        din.readFully(content);
        System.out.println("Encrypted image data recieved of length: "+content.length);
        System.out.println("Decrypting the image...");
		byte[] dec=decrypt(content,nkey);
        System.out.println("Image decrypted and is of length: "+dec.length);
        System.out.println("Enter the destination path of the decrypted image (Enter image name if in same folder):");
        String destimg=s.nextLine();
        FileOutputStream fos=new FileOutputStream(destimg);
        fos.write(dec);
        System.out.println("Exiting");
        fos.close();
        ss.close();
        s.close();
		
		
		
		
	}

}
