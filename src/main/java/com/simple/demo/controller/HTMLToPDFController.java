package com.simple.demo.controller;

import java.io.IOException;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.lowagie.text.DocumentException;
import com.simple.demo.service.HTMLToPDFService;

@RestController
public class HTMLToPDFController {
	
	
	@Autowired
	private HTMLToPDFService htmlToPDFService;
	
	@GetMapping(value = "/convert-html-to-pdf/{jdId}/{resumeId}")
	public ResponseEntity<String> covertHTMLToPDF(@PathVariable long jdId, @PathVariable long resumeId) throws DocumentException, IOException, JSONException{
	return ResponseEntity.ok(htmlToPDFService.HTMLToPDFConvert(jdId, resumeId));
	}

}
