import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONObject;

import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;

public class Test {

	public static void main(String[] args){
		try{
		testmethod();
		}catch(Exception e){System.out.print(e.getCause().toString());}
	}
	public static void testmethod(){
		int[]abab={0,2};
		abab[3]=2;
	}

}
