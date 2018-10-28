import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONObject;
import edu.self.kraken.api.KrakenApi;
import edu.self.kraken.api.KrakenApi.Method;

public class Trader {
	static Scanner scan;
	static Scanner in;
	static PrintWriter writer;
	static Currency []prices;
	static final String [] assets= new String[]{"XXBTZUSD","BCHUSD","DASHUSD","EOSUSD","XETCZUSD","GNOUSD","XLTCZUSD","XREPZUSD","USDTZUSD","XXLMZUSD","XXMRZUSD","XXRPZUSD","XZECZUSD"};
	static KrakenApi api;
	static String res="";
	static double [] vals;

	public static void main(String[]args) throws InvalidKeyException, NoSuchAlgorithmException, IOException, AddressException, MessagingException{


		prices = new Currency [assets.length];

		api = new KrakenApi();
		api.setKey(Val.key); // FIXME
		api.setSecret(Val.secret); // FIXME

		File data = new File("data.txt");
		if(data.createNewFile()){}
		File values = new File("values.txt");
		if(values.createNewFile()){}
		File balances = new File("balances.txt");
		if(balances.createNewFile()){}
		
		scan = new Scanner(data);
		in = new Scanner(values);
		if(!readData()){
			for(int i=0;i<prices.length;i++){
				prices[i]=new Currency(-1,-1,-1,-1,100000000,assets[i],-1,100000000);
			}
		}
		writer= new PrintWriter(data);


		generateTradeValues();
		getBalances();
		log();
		putData();
		trade();

		PrintWriter output = new PrintWriter(balances);

		for(int i=0;i<vals.length;i++){

			output.println(""+vals[i]);
		}

		output.close();






	}

	public static boolean readData(){
		if(!scan.hasNext()){
			System.out.println("file empty");
			return false;
		}
		else{
			for(int i=0;i<assets.length;i++){
				scan.next();
				prices[i]=new Currency(scan.nextDouble(), scan.nextDouble(), scan.nextDouble(), scan.nextDouble(), scan.nextDouble(), assets[i],scan.nextDouble(),scan.nextDouble());
			}

		}
		return true;
	}

	public static void putData(){
		writer.flush();
		double max = -100000000;
		double min = 100000000;
		int maxIndex=-1;
		int minIndex=-1;
		for(int i=0;i<assets.length;i++){
			//asset pair new val old val ratio weeklymax weeklymin currentmax currentmin
			writer.printf("%s %.5f %.5f %.3f %.5f %.5f %.5f %.5f\n",assets[i],prices[i].getNewVal(),prices[i].getOldVal(),prices[i].getRatio(),prices[i].getWeeklyMax(),prices[i].getWeeklyMin(),prices[i].getCurrentMax(),prices[i].getCurrentMin());
			if(prices[i].getRatio()>max){
				max=prices[i].getRatio();
				maxIndex=i;
			}
			if(prices[i].getRatio()<min){
				min=prices[i].getRatio();
				minIndex=i;
			}
		}
		System.out.println("max======"+max+" "+assets[maxIndex]+" min======="+min+" "+assets[minIndex]);
	}

	public static void log() throws IOException{


		for(int i=0;i<assets.length;i++){
			String response;
			Map<String, String> input = new HashMap<>();

			input.clear();
			String asset=assets[i];
			input.put("pair",asset);

			response = api.queryPublic(Method.TICKER, input);
			System.out.println(response);

			JSONObject obj = new JSONObject(response);
			double ask = obj.getJSONObject("result").getJSONObject(asset).getJSONArray("a").getDouble(0);
			double bid= obj.getJSONObject("result").getJSONObject(asset).getJSONArray("b").getDouble(0);
			System.out.println("sell price===="+ask);
			System.out.println("buy===="+bid);
			System.out.println("avg====="+((ask+bid)/2.0));

			prices[i].setOldVal(prices[i].getNewVal());
			prices[i].setNewVal(((ask+bid)/2.0));
			double diff = prices[i].getNewVal()/prices[i].getOldVal();
			System.out.println(diff);
			prices[i].setRatio(diff<1.0 ? diff-1 : diff); 

			if(prices[i].getNewVal()<prices[i].getCurrentMin())
				prices[i].setCurrentMin(prices[i].getNewVal());
			if(prices[i].getNewVal()>prices[i].getCurrentMax())
				prices[i].setCurrentMax(prices[i].getNewVal());

		}
	}
	public static void generateTradeValues() throws IOException{
		for(int i=0;i<assets.length;i++)
			prices[i].generateValues(api);


	}
	public static void trade() throws AddressException, MessagingException, FileNotFoundException{
		String result="";
		int numPositive=0;
		int numNegative=0;
		for(int i=0;i<assets.length;i++){
			System.out.println("current val=="+prices[i].getNewVal()+" weekly vals=="+prices[i].getWeeklyMax()+"  "+prices[i].getWeeklyMin());
			if(prices[i].getNewVal()<prices[i].getWeeklyMin()){
				result+="BUY "+assets[i]+"\n";
				result+=" Current Price: "+prices[i].getNewVal()+"\n";
				result+=" Weekly Min:"+prices[i].getWeeklyMin()+"\n";
				result+=" Horuly Ratio:"+prices[i].getRatio()+"\n";
				System.out.println("BUY "+assets[i]);
				ExecuteTrade("BUY",i);
			}
			if(prices[i].getNewVal()>prices[i].getWeeklyMax()){
				System.out.println("SELL "+assets[i]);
				result+="SELL "+assets[i];
				result+=" Current Price: "+prices[i].getNewVal()+"\n";
				result+=" Weekly Max:"+prices[i].getWeeklyMax()+"\n";
				result+=" Horuly Ratio:"+prices[i].getRatio()+"\n";
				result+=" Weekly Ratio:"+ (prices[i].getNewVal()/prices[i].getWeeklyMax())+String.format("%n");
				ExecuteTrade("SELL",i);
			}
			if(prices[i].getRatio()>0)
				numPositive++;
			else if(prices[i].getRatio()<0)
				numNegative++;
		}

		if(numPositive>numNegative)
			result+="General Trend: Up\n";
		else if(numPositive<numNegative)
			result+="General Trend: Down\n";

		for(int i=0;i<vals.length-1;i++){
			if(vals[i]!=0){
				result+="\n"+assets[i]+" "+vals[i];
			}
		}
		result+="\n USD "+vals[vals.length-1];
		System.out.println(result);
		if(!result.equals(""))
			sendMail(result);
	}

	public static void sendMail(String msg) throws AddressException, MessagingException{
		String to="gula1214@gmail.com";
		String from="gula1214@gmail.com";
		Properties props = System.getProperties();
		String host = "smtp.gmail.com";
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", "gula1214@gmail.com");
		props.put("mail.smtp.password", Val.pwd);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");

		Session session = Session.getDefaultInstance(props);
		MimeMessage message = new MimeMessage(session);


		message.setFrom(new InternetAddress("gula1214@gmail.com"));
		InternetAddress toAddress = new InternetAddress(to);

		// To get the array of addresses



		message.addRecipient(Message.RecipientType.TO, toAddress);


		message.setSubject("CryptoAlert");
		message.setText(msg);

		Transport transport = session.getTransport("smtp");
		transport.connect(host, from, Val.pwd);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();

	}
	public static void getBalances() throws FileNotFoundException{
		File file = new File("balances.txt");
		Scanner scan = new Scanner(file);
		vals =  new double[assets.length+1];
		try{
			if(scan.hasNext()){
				for(int i=0;i<vals.length;i++){
					vals[i]=scan.nextDouble();
				}
			}
			else
				throw new IOException();
		}catch(Exception e){
			for(int i=0;i<vals.length-1;i++){
				vals[i]=0;
			}
			vals[vals.length-1]=100001;
		}
	}
	public static void ExecuteTrade(String request, int pairIndex) throws FileNotFoundException{

		if(request.equals("SELL")){
			if(vals[pairIndex]>0){
				vals[assets.length]+=vals[pairIndex]*prices[pairIndex].getNewVal();
				vals[pairIndex]=0;
			}
		}
		else if(request.equals("BUY")){
			if(vals[assets.length]>0){
				vals[pairIndex]+=vals[assets.length]/prices[pairIndex].getNewVal();
				vals[assets.length]=0;
			}
		}




	}


}
class Currency{
	private double oldVal;
	private double newVal;
	private double ratio;
	private double weeklyMin;
	private double weeklyMax;
	private double currentMin;
	private double currentMax;
	private String assetName;



	Currency(double newVal, double oldVal, double ratio, double weeklyMax, double weeklyMin, String assetName, double currentMin, double currentMax) {
		super();
		this.oldVal = oldVal;
		this.newVal = newVal;
		this.ratio = ratio;
		this.weeklyMin = weeklyMin;
		this.weeklyMax = weeklyMax;
		this.assetName=assetName;
		this.currentMin=currentMin;
		this.currentMax=currentMax;
	}

	public double getOldVal() {
		return oldVal;
	}
	public void setOldVal(double oldVal) {
		this.oldVal = oldVal;
	}
	public double getNewVal() {
		return newVal;
	}
	public void setNewVal(double newVal) {
		this.newVal = newVal;
	}
	public double getRatio() {
		return ratio;
	}
	public void setRatio(double ratio) {
		this.ratio = ratio;
	}
	public double getWeeklyMin() {
		return weeklyMin;
	}
	public void setWeeklyMin(double weeklyMin) {
		this.weeklyMin = weeklyMin;
	}
	public double getWeeklyMax() {
		return weeklyMax;
	}
	public void setWeeklyMax(double weeklyMax) {
		this.weeklyMax = weeklyMax;
	}

	public String getAssetName() {
		return assetName;
	}

	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}

	public double getCurrentMin() {
		return currentMin;
	}

	public void setCurrentMin(double currentMin) {
		this.currentMin = currentMin;
	}

	public double getCurrentMax() {
		return currentMax;
	}

	public void setCurrentMax(double currentMax) {
		this.currentMax = currentMax;
	}

	public void generateValues(KrakenApi api) throws IOException{
		String response;
		Map<String, String> input = new HashMap<>();
		input.clear();
		input.put("pair",this.assetName);
		input.put("interval", "10080");
		int sinceVal= Integer.parseInt((new JSONObject(api.queryPublic(Method.TIME)).getJSONObject("result").optString("unixtime")));
		input.put("since",""+(sinceVal-604800));
		response = api.queryPublic(Method.OHLC,input);
		System.out.println(response);
		JSONObject obj = new JSONObject(response);
		this.weeklyMin=obj.getJSONObject("result").getJSONArray(this.assetName).getJSONArray(0).optDouble(3);
		this.weeklyMax=obj.getJSONObject("result").getJSONArray(this.assetName).getJSONArray(0).optDouble(2);

	}
}
