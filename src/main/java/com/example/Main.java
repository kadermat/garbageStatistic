/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@SpringBootApplication
public class Main {

	private static final String BASE_URL_GOOGLE_MAPS = "https://maps.googleapis.com/maps/api/staticmap?";
	
	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Autowired
	private DataSource dataSource;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String index(HttpServletRequest request) throws IOException {
		System.out.println("Accept: " + request.getHeader("Accept"));
		System.out.println("testAttr: " + request.getHeader("Test"));
		// System.out.println(getApikey());

		FileOutputStream fos;

		// fos = new FileOutputStream("E:\\googleDrive\\test.png");
		// fos.write(getGoogleMapsImage());

		// fos.close();

		return "index";

	}

	@RequestMapping(value = "/test", method = RequestMethod.GET, produces = "image/png")
	public @ResponseBody byte[] picture(HttpServletRequest request) throws IOException {

		return getGoogleMapsImage();

	}

	/**
	 * Gets the api key for google maps out of the database,
	 * 
	 * @return api key if successfull, null otherise
	 */
	private String getApikey() {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * from api");

			String key = null;
			while (rs.next()) {
				key = rs.getString(2);
			}

			return key;
		} catch (Exception e) {
			System.out.println("exception getting api key");
			return null;
		}

	}

	private byte[] getGoogleMapsImage() throws IOException {
		URL url = new URL(
				"https://maps.googleapis.com/maps/api/staticmap?center=\"47.405045,8.403371\"&size=1920x1080&scale=1&maptype=roadmap4&key=AIzaSyCsM3ePZ60a1PGKOgcxhQmi94QX2yGNZgE");
		InputStream in = new BufferedInputStream(url.openStream());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n = 0;
		while (-1 != (n = in.read(buf))) {
			out.write(buf, 0, n);
		}
		out.close();
		in.close();
		byte[] response = out.toByteArray();
		return response;
	}
	
	private String requestUrlBuilder(String center, String size, String scale, List<String> markers) {
		String url = BASE_URL_GOOGLE_MAPS;
		String key = getApikey();
		
		
		
		return null;
	}
	

	@RequestMapping("/db")
	public String db(Map<String, Object> model) {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
			stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
			ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getTimestamp("tick"));
			}

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}

	@Scheduled(fixedDelay = 500000)
	private void generateNewImage() throws IOException {

		FileOutputStream out = new FileOutputStream(new File("test.png"));
		out.write(getGoogleMapsImage());
		out.close();
		System.out.println("Got new Image");
	}

	@Bean
	public DataSource dataSource() throws SQLException {
		if (dbUrl == null || dbUrl.isEmpty()) {
			return new HikariDataSource();
		} else {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(dbUrl);
			return new HikariDataSource(config);
		}
	}

}
