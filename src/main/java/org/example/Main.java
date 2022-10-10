package org.example;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;


public class Main {
	public static void main(String[] args) {
		JSONObject inputJson = readInputJson();
		if(inputJson == null){
			System.out.println("Can't read input file! Ensure that there is file dataToSent.json in System.getProperty(\"user.dir\")");
			return;
		}

		JSONObject response = sendRequestToServer(inputJson);
		if(response == null){
			System.out.println("Can't get response from server!");
			return;
		}

		if(!validateResponse(response)){
			System.out.println("Response from server is invalid!");
			return;
		}

		printInfoFromResponse(response);
	}

	static private JSONObject readInputJson(){
		String path = "./dataToSent.json";
		JSONObject inputJson = null;
		try {
			String inputJsonStr = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
			inputJson = new JSONObject(inputJsonStr);

			if(!inputJson.has("CustomersPrices"))
				throw new RuntimeException("No CustomersPrices field");
			if(!inputJson.has("EconomyRoomsNumber"))
				throw new RuntimeException("No EconomyRoomsNumber field");
			if(!inputJson.has("PremiumRoomsNumber"))
				throw new RuntimeException("No PremiumRoomsNumber field");

			JSONArray CustomerPrices = inputJson.getJSONArray("CustomersPrices");
			int EconomyRoomsNumber = inputJson.getInt("EconomyRoomsNumber");
			int PremiumRoomsNumber = inputJson.getInt("PremiumRoomsNumber");
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			System.out.println("Error, in reading " + path + "[" + e + "]");
			throw new RuntimeException(e);
		}
		return inputJson;
	}
	static private JSONObject sendRequestToServer(JSONObject inputDataJson){
		final MediaType JsonMediaType = MediaType.get("application/json; charset=utf-8");
		final String url = "https://roommanagerjobtask.azurewebsites.net/assignRooms";
		try {
			RequestBody requestBody = RequestBody.create(inputDataJson.toString(), JsonMediaType);
			Request request = new Request.Builder().url(url).post(requestBody).build();

			OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3000, TimeUnit.MILLISECONDS).build();
			Response response = client.newCall(request).execute();
			if(response.code() != 200){
				String msg = "Error in connecting to server, server returned code " + response.code();
				JSONObject responseBody = new JSONObject(response.body().string());
				if(responseBody.has("ErrorMsg"))
					msg += " Error msg: "+ responseBody.getString("ErrorMsg");
				throw new Exception(msg);
			}
			return new JSONObject(response.body().string());
		}catch (Exception e){
			System.out.println(e);
			return null;
		}
	}
	static private boolean validateResponse(JSONObject responseBody){
		if(!responseBody.has("PremiumRoomsPrices"))
			return false;
		if(!responseBody.has("EconomyRoomsPrices"))
			return false;
		if(!responseBody.has("RejectedPrices"))
			return false;
		if(!responseBody.has("TotalIncome"))
			return false;

		return true;
	}
	static private void printInfoFromResponse(JSONObject responseBody){
		JSONArray premiumRoomCustomers = responseBody.getJSONArray("PremiumRoomsPrices");
		JSONArray economyRoomCustomers = responseBody.getJSONArray("EconomyRoomsPrices");
		JSONArray rejectedCustomers = responseBody.getJSONArray("RejectedPrices");
		double totalIncome = responseBody.getDouble("TotalIncome");

		System.out.println("Customers who want to pay following prices should be assigned to premium rooms:");
		System.out.println(premiumRoomCustomers.toString());
		System.out.println();
		System.out.println("Customers who want to pay following prices should be assigned to economy rooms:");
		System.out.println(economyRoomCustomers.toString());
		System.out.println();
		System.out.println("Customers who want to pay following prices should be rejected:");
		System.out.println(rejectedCustomers.toString());
		System.out.println();
		System.out.println("Total income: " + String.format("%.2f", totalIncome));
		System.out.println("Occupied economy rooms: " + economyRoomCustomers.length());
		System.out.println("Occupied premium rooms: " + premiumRoomCustomers.length());

	}
}