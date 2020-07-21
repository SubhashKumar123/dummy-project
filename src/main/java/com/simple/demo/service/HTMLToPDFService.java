package com.simple.demo.service;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lowagie.text.DocumentException;
import com.simple.demo.constant.Constant;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Service
public class HTMLToPDFService {
	
	@Value("${offer.pdf.directory}")
	private String offerDir;

	private static final String UTF_8 = "UTF-8";

	private static final String[] specialNames = { "", " thousand", " million", " billion", " trillion", " quadrillion",
			" quintillion" };

	private static final String[] tensNames = { "", " ten", " twenty", " thirty", " forty", " fifty", " sixty",
			" seventy", " eighty", " ninety" };

	private static final String[] numNames = { "", " one", " two", " three", " four", " five", " six", " seven",
			" eight", " nine", " ten", " eleven", " twelve", " thirteen", " fourteen", " fifteen", " sixteen",
			" seventeen", " eighteen", " nineteen" };

	public String HTMLToPDFConvert(long jdId, long resumeId) throws DocumentException, IOException, JSONException {

		String token = callTokenAPI();
		// Parse the Json response and retrieve the Access Token
		Gson gson = new Gson();
		Type mapType = new TypeToken<Map<String, String>>() {
		}.getType();
		Map<String, String> ser = gson.fromJson(token, mapType);
		String accessToken = ser.get("access_token");

		String apiOutput = callRestApI(accessToken, jdId, resumeId);

		return parseThymeleafTemplate(apiOutput, jdId, resumeId);
	}

	public String parseThymeleafTemplate(String apiOutput,long jdId, long resumeId) throws DocumentException, IOException, JSONException {

		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode("HTML");
		templateResolver.setCharacterEncoding(UTF_8);

		TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);

		Context context = new Context();

		JSONObject object = new JSONObject(apiOutput);
		JSONObject location = object.getJSONObject("location");
		JSONObject state = new JSONObject(location.getString("state"));
		JSONObject country = new JSONObject(location.getString("country"));
		String words = convert(object.getInt("amount"));

		JSONArray jsonArray = object.getJSONArray("salaryStructure");

		for (int i = 0; i < jsonArray.length(); i++) {

			JSONObject objects = jsonArray.getJSONObject(i);

			if (objects.getString("name").trim().equalsIgnoreCase("Part A(Fixed Components)".trim())) {

				JSONArray rows = objects.getJSONArray("rows");

				for (int j = 0; j < rows.length(); j++) {
					JSONObject partAArray = rows.getJSONObject(j);
					if (partAArray.getString("name").trim().equalsIgnoreCase("Basic".trim())) {
						context.setVariable("basicMonthly", partAArray.get("monthly"));
						context.setVariable("basicAnnual", partAArray.get("annual"));
					} else if (partAArray.getString("name").trim().equalsIgnoreCase("HRA".trim())) {
						context.setVariable("HRAMonthly", partAArray.get("monthly"));
						context.setVariable("HRAAnnual", partAArray.get("annual"));
					} else if (partAArray.getString("name").trim().equalsIgnoreCase("Compenatory Allowance".trim())) {
						context.setVariable("compenatoryAllowanceMonthly", partAArray.get("monthly"));
						context.setVariable("compenatoryAllowanceAnnual", partAArray.get("annual"));
					}

				}

			} else if (objects.getString("name").trim().equalsIgnoreCase("Part B(Allowance)".trim())) {
				JSONArray rows = objects.getJSONArray("rows");
				for (int j = 0; j < rows.length(); j++) {
					JSONObject partBArray = rows.getJSONObject(j);
					if (partBArray.getString("name").trim().equalsIgnoreCase("Internet Allowance".trim())) {
						context.setVariable("internetAllowanceMonthly", partBArray.get("monthly"));
						context.setVariable("internetAllowanceAnnual", partBArray.get("annual"));
					} else if (partBArray.getString("name").trim().equalsIgnoreCase("Medical Allowance".trim())) {
						context.setVariable("medicalAllowanceMonthly", partBArray.get("monthly"));
						context.setVariable("medicalAllowanceAnnual", partBArray.get("annual"));
					}
				}

			} else if (objects.getString("name").trim().equalsIgnoreCase("Part C(MPLB)".trim())) {
				JSONArray rows = objects.getJSONArray("rows");
				for (int j = 0; j < rows.length(); j++) {
					JSONObject partCArray = rows.getJSONObject(j);
					if (partCArray.getString("name").trim()
							.equalsIgnoreCase("Monthly performance Linked Bonus".trim())) {
						context.setVariable("monthlyPerformanceLinkedBonus", partCArray.get("monthly"));
						context.setVariable("monthlyPerformanceLinkedBonusAnnual", partCArray.get("annual"));
					} else if (partCArray.getString("name").trim().equalsIgnoreCase("grossSalary".trim())) {
						context.setVariable("grossSalaryMonthly", partCArray.get("monthly"));
						context.setVariable("grossSalaryMonthlyAnnual", partCArray.get("annual"));

					}
				}

			} else if (objects.getString("name").trim().equalsIgnoreCase("Part D(APLB)".trim())) {
				JSONArray rows = objects.getJSONArray("rows");
				for (int j = 0; j < rows.length(); j++) {
					JSONObject partDArray = rows.getJSONObject(j);
					if (partDArray.getString("name").trim()
							.equalsIgnoreCase("Annual performance Linked Bonus".trim())) {
						context.setVariable("annualPerformanceLinkedBonusMonthly", partDArray.get("monthly"));
						context.setVariable("annualPerformanceLinkedBonusAnnual", partDArray.get("annual"));
					}
				}
			} else if (objects.getString("name").trim().equalsIgnoreCase("Part E(Retirals & Benefits)".trim())) {
				JSONArray rows = objects.getJSONArray("rows");
				for (int j = 0; j < rows.length(); j++) {
					JSONObject partEArray = rows.getJSONObject(j);
					if (partEArray.getString("name").trim()
							.equalsIgnoreCase("PF(Provident Fund)-Employer Contribution".trim())) {
						context.setVariable("PFMonthly", partEArray.get("monthly"));
						context.setVariable("PFAnnual", partEArray.get("annual"));
					} else if (partEArray.getString("name").trim().equalsIgnoreCase("Gratuity".trim())) {
						context.setVariable("gratuityMonthly", partEArray.get("monthly"));
						context.setVariable("gratuityAnnual", partEArray.get("annual"));
					} else if (partEArray.getString("name").trim().equalsIgnoreCase("Insurance Benefits".trim())) {
						context.setVariable("insuranceBenefitsMonthly", partEArray.get("monthly"));
						context.setVariable("insuranceBenefitsAnnual", partEArray.get("annual"));
					} else if (partEArray.getString("name").trim().equalsIgnoreCase("totalCTC".trim())) {
						context.setVariable("totalCTCMonthly", partEArray.get("monthly"));
						context.setVariable("totalCTCAnnual", partEArray.get("annual"));
					}
				}
			}
		}

		context.setVariable("officeLocation", location.get("addressLineOne") + ", " + state.getString("stateName") + ","
				+ country.getString("niceName"));
		context.setVariable("country", country.getString("niceName"));
		context.setVariable("candidateName", object.getString("candidateName"));
		context.setVariable("joiningDate", object.getString("joiningDate"));
		context.setVariable("address", object.getString("address"));
		context.setVariable("ctc", object.getString("amount"));
		context.setVariable("words", words);
		context.setVariable("refId", object.getString("referenceId"));
		context.setVariable("currDate", LocalDate.now());

		// Get the plain HTML with the resolved ${name} variable!
		String renderedHtmlContent = templateEngine.process("template", context);
		generatePdfFromHtml(renderedHtmlContent, jdId, resumeId);

		return "convert";

	}

	public void generatePdfFromHtml(String HTML,long jdId, long resumeId) throws DocumentException, IOException {
		String fileName = jdId+","+resumeId;
		//OutputStream outputStream = new FileOutputStream("D:\\message.pdf");
		OutputStream outputStream = new FileOutputStream(offerDir+"\\"+fileName+".pdf");
		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocumentFromString(HTML);
		renderer.layout();
		renderer.createPDF(outputStream);
		outputStream.close();
	}

	public String callTokenAPI() {
		Client client = Client.create();
		final HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(Constant.authorizationUser,
				Constant.authorizationPassword);
		client.addFilter(authFilter);
		client.addFilter(new LoggingFilter());

		WebResource webResource = client.resource(Constant.tokenURL);

		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("username", Constant.username);
		formData.add("password", Constant.password);
		formData.add("grant_type", Constant.grant_type);

		ClientResponse response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class,
				formData);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);
		return output;

	}

	public String callRestApI(String accessToken, long jdId, long resumeId) {

		Client client = Client.create();
		client.addFilter(new LoggingFilter());
		WebResource webResource = client.resource(Constant.URL).path(String.valueOf(jdId))
				.path(String.valueOf(resumeId));

		String value = "Bearer " + accessToken;

		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).header("Authorization", value)
				.get(ClientResponse.class);

		if (response.getStatus() != 200) {
			System.out.println("Failed : HTTP error code : " + response.getStatus());
			return "Error";
		}

		String output = response.getEntity(String.class);
		System.err.println("outPut=" + output);
		return output;

	}

	public static String convert(int number) {

		if (number == 0) {
			return "zero";
		}

		String prefix = "";

		if (number < 0) {
			number = -number;
			prefix = "negative";
		}

		String current = "";
		int place = 0;

		do {
			int n = number % 1000;
			if (n != 0) {
				String s = convertLessThanOneThousand(n);
				current = s + specialNames[place] + current;
			}
			place++;
			number /= 1000;
		} while (number > 0);

		return (prefix + current).trim();
	}

	private static String convertLessThanOneThousand(int number) {
		String current;

		if (number % 100 < 20) {
			current = numNames[number % 100];
			number /= 100;
		} else {
			current = numNames[number % 10];
			number /= 10;

			current = tensNames[number % 10] + current;
			number /= 10;
		}
		if (number == 0)
			return current;
		return numNames[number] + " hundred" + current;
	}

}
