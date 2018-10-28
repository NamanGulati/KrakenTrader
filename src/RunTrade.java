import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

public class RunTrade {
		
	static boolean refreshValue=false;
	
	public static void main(String[] args) throws IOException, AddressException, MessagingException {
		// TODO Auto-generated method stub
		while(true){
			
			try {
				Trader.main(null);
			}
			catch (Exception e){
				
				e.printStackTrace();
				//Trader.sendMail("errorrrr \n"+e.getCause().toString());
			}

			
			try {
				Thread.sleep(3600000*3);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		
			
			
			
		}
	}

}
