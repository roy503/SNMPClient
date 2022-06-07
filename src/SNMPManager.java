import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.snmp4j.*;
import org.snmp4j.event.*;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;


public class SNMPManager {

Snmp snmp;
ArrayList<String> addressList;
ArrayList<Printer> printers;
/**
* Constructor
* @param add
*/
private SNMPManager()
{
	addressList = new ArrayList<String>();
	printers = new ArrayList<Printer>();
	snmp = null;
	
}

public static void main(String[] args){
	SNMPManager Manager = new SNMPManager();
	Manager.begin();
}
private void begin() {
/**
* Port 161 is used for Read and Other operations
* Port 162 is used for the trap generation
*/
	inputAddresses();
	start();
	System.out.println(String.format("%-30s %-16s %-70s %-20s %2s", "Location","IP","Model","Serial","Toner"));
	for(int i = 0;i < printers.size();i++) {
		getAsString(printers.get(i));
	}
	try {
		snmp.close();
	} catch (IOException e) {
		// Error
		System.out.println("Error trying to close() snmp");
		System.exit(1);
	}
}

private void inputAddresses(){
	//input addresses from text file
	try(InputStream in = SNMPManager.class.getResourceAsStream("printers.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
		String line;
		while((line = reader.readLine()) != null) {
			//while line exists, add to list
			Printer newPrinter = new Printer(line);
			addressList.add(line);
			printers.add(newPrinter);
		}
	} catch (IOException | NullPointerException e) {
		//error
		System.out.println("Can't find printers.txt");
		System.exit(1);
	} 
}

/**
* Start the Snmp session. If you forget the listen() method you will not
* get any answers because the communication is asynchronous
* and the listen() method listens for answers.
* @throws IOException
*/
private void start() {
	TransportMapping<?> transport;
	try {
		transport = new DefaultUdpTransportMapping();
		snmp = new Snmp(transport);
		snmp.listen();
	} catch (IOException e) {
		// Error
		System.out.println("Error trying to listen()");
		System.exit(1);
	}
}

/**
* Method which takes a single OID and returns the response from the agent as a String.
* @param oid
* @return
* @throws IOException
*/
private void getAsString(Printer obj){

	//These care custom IP settings for the printers on my network
	ResponseEvent<?> event;
	//colour printer
	if(("10.214.192.87").equals(obj.getIP()) || ("10.214.192.95").equals(obj.getIP())) {
		event = get(new OID[] {new OID(".1.3.6.1.2.1.25.3.2.1.3.1"),new OID(".1.3.6.1.2.1.43.11.1.1.9.1.1"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.1"),
				new OID(".1.3.6.1.2.1.43.11.1.1.9.1.2"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.2"),new OID(".1.3.6.1.2.1.43.11.1.1.9.1.3"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.3"),
				new OID(".1.3.6.1.2.1.43.11.1.1.9.1.4"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.4"),new OID(".1.3.6.1.2.1.1.6.0"),new OID(".1.3.6.1.2.1.43.5.1.1.17.1")}, obj);	
		//0 name, 1 cyan curr, 2 cyan max, 3 magenta curr, 4 magenta max, 5yellow curr, 6yellow max, 7 black curr, 8 black max, 9 location, 10 serial
	}
	//Print room printers use 2 toners, K1 and K2
	else if(("10.214.192.79").equals(obj.getIP()) || ("10.214.192.91").equals(obj.getIP())){
		event = get(new OID[] { new OID(".1.3.6.1.2.1.25.3.2.1.3.1"),new OID(".1.3.6.1.2.1.43.11.1.1.9.1.30"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.30"),new OID(".1.3.6.1.2.1.43.11.1.1.9.1.31"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.31"), new OID(".1.3.6.1.2.1.1.6.0"),new OID(".1.3.6.1.2.1.43.5.1.1.17.1")}, obj);
		//0 name, 1 K1 Curr, 2 K1 Max, 3 K2 Curr, 4 K2 Max, 5 Location, 6 Serial
	}
	//Office label printer
	else if(("10.214.192.250").equals(obj.getIP())) {
		event = get(new OID[] { new OID(".1.3.6.1.2.1.25.3.2.1.3.1"), new OID(".1.3.6.1.2.1.1.6.0")}, obj);
		//0 desc, 1 location
	}
	//All other printers
	else {
		event = get(new OID[] { new OID(".1.3.6.1.2.1.25.3.2.1.3.1"),new OID(".1.3.6.1.2.1.43.11.1.1.9.1.1"),new OID(".1.3.6.1.2.1.43.11.1.1.8.1.1"),new OID(".1.3.6.1.2.1.1.6.0"),new OID(".1.3.6.1.2.1.43.5.1.1.17.1")}, obj);
		// 0 name, 1 tonerCurr, 2 tonerMax, 3 location, 4 serial
	}
	//not sure if this toner works for colour printers?
	if(event.getResponse() != null) {
		//Print room printers
		if(("10.214.192.87").equals(obj.getIP()) || ("10.214.192.95").equals(obj.getIP()) ) {
			//colour printer
			//0 name, 1 cyan curr, 2 cyan max, 3 magenta curr, 4 magenta max, 5yellow curr, 6yellow max, 7 black curr, 8 black max, 9 location, 10 serial
			obj.setName(event.getResponse().get(0).getVariable().toString());
			obj.setCyan(Math.round(Float.parseFloat(event.getResponse().get(1).getVariable().toString())/Float.parseFloat(event.getResponse().get(2).getVariable().toString())*100));
			obj.setMagenta(Math.round(Float.parseFloat(event.getResponse().get(3).getVariable().toString())/Float.parseFloat(event.getResponse().get(4).getVariable().toString())*100));
			obj.setYellow(Math.round(Float.parseFloat(event.getResponse().get(5).getVariable().toString())/Float.parseFloat(event.getResponse().get(6).getVariable().toString())*100));
			obj.setBlack(Math.round(Float.parseFloat(event.getResponse().get(7).getVariable().toString())/Float.parseFloat(event.getResponse().get(8).getVariable().toString())*100));
			if(("10.214.192.95").equals(obj.getIP())) {
				obj.setLocation("Careers Office Colour (FR0037)");
			}
			else {
				obj.setLocation(event.getResponse().get(9).getVariable().toString());
			}
			obj.setSerial(event.getResponse().get(10).getVariable().toString());
			obj.setColour();
			System.out.println(obj.toString());
		}
		else if(("10.214.192.79").equals(obj.getIP()) || ("10.214.192.91").equals(obj.getIP())) {
			//print room printers
			float tonerK1 = Float.parseFloat(event.getResponse().get(1).getVariable().toString())/Float.parseFloat(event.getResponse().get(2).getVariable().toString());
			float tonerK2 = Float.parseFloat(event.getResponse().get(3).getVariable().toString())/Float.parseFloat(event.getResponse().get(4).getVariable().toString());
			obj.setK1(Math.round(tonerK1*100));
			obj.setK2(Math.round(tonerK2*100));
			obj.setName(event.getResponse().get(0).getVariable().toString());
			obj.setLocation(event.getResponse().get(5).getVariable().toString());
			obj.setSerial(event.getResponse().get(6).getVariable().toString());
			obj.setPrintRoom();
			System.out.println(obj.toString());
		}
		//label printer
		else if(("10.214.192.250").equals(obj.getIP())) {
			obj.setName(event.getResponse().get(0).getVariable().toString());
			obj.setLocation(event.getResponse().get(1).getVariable().toString());
			obj.setLabelPrinter();
			System.out.println(obj.toString());
		}
		else if(("10.214.192.76").equals(obj.getIP()) || ("10.214.192.97").equals(obj.getIP())) {
			//p2015 printers location fix
			float tonerP = Float.parseFloat(event.getResponse().get(1).getVariable().toString())/Float.parseFloat(event.getResponse().get(2).getVariable().toString());
			obj.setBlack(Math.round(tonerP*100));
			obj.setName(event.getResponse().get(0).getVariable().toString());
			if(("10.214.192.76").equals(obj.getIP())) {
				obj.setLocation("LAST Office (FR0030)");
			}
			else if(("10.214.192.97").equals(obj.getIP())) {
				obj.setLocation("TSO Office (FR0091)");
			}
			obj.setSerial(event.getResponse().get(4).getVariable().toString());
			System.out.println(obj.toString());
			
		}
		else {
			//all others
			float tonerP = Float.parseFloat(event.getResponse().get(1).getVariable().toString())/Float.parseFloat(event.getResponse().get(2).getVariable().toString());
			obj.setBlack(Math.round(tonerP*100));
			obj.setName(event.getResponse().get(0).getVariable().toString());
			obj.setLocation(event.getResponse().get(3).getVariable().toString());
			obj.setSerial(event.getResponse().get(4).getVariable().toString());
			System.out.println(obj.toString());
		}
	}
	else {
		//can't contact printer
		obj.setOffline();
		System.out.println(obj.toString());
		}
}

/**
* This method is capable of handling multiple OIDs
* @param oids
* @return
* @throws IOException
*/
private ResponseEvent<?> get(OID oids[], Printer obj){
	PDU pdu = new PDU();
	for (OID oid : oids) {
		pdu.add(new VariableBinding(oid));
	}
	pdu.setType(PDU.GET);
	ResponseEvent<?> event = null;
	try {
		event = snmp.send(pdu, getTarget(obj, 2), null);
	} catch (IOException e) {
		// Error on V2
	}
	//try v2c
	if(event.getResponse() != null) {
		//v2c works
		return event;
	}
	else {
		//v2c timed out
		try {
			event = snmp.send(pdu, getTarget(obj, 1), null);
		} catch (IOException e) {
			// Error on V1
		}
		if(event.getResponse() != null) {
			//v1 works
			return event;
		}
		else {
			//v1 timed out
		}
	}
	//GET timed out
	return event;
}

/**
* This method returns a Target, which contains information about
* where the data should be fetched and how.
* @return
*/
private CommunityTarget<Address> getTarget(Printer obj, int ver) {
	Address targetAddress = GenericAddress.parse("udp:"+obj.getIP()+"/161");
	CommunityTarget<Address> target = new CommunityTarget<Address>();
	target.setCommunity(new OctetString("public"));
	target.setAddress(targetAddress);
	target.setRetries(2);
	target.setTimeout(1500);
	//SNMP Version 1, 2c, or 3
	if(ver==2) {
		target.setVersion(SnmpConstants.version2c);
	}
	else if(ver==1) {
		target.setVersion(SnmpConstants.version1);
	}
	return target;
}
}
