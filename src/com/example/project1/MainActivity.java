package com.example.project1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

@SuppressLint("DefaultLocale")
public class MainActivity extends Activity implements LocationListener {

	// Holds the latitude and longitude for refresh
	Double latitude_;
	Double longitude_;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Sets the textviews to invisible, they don't look good if they start
		// up
		TextView todayTextView = (TextView) findViewById(R.id.todayText);
		TextView tomorrowTextView = (TextView) findViewById(R.id.tomorrowText);
		todayTextView.setVisibility(View.GONE);
		tomorrowTextView.setVisibility(View.GONE);

		// Listener for an enter click in the input box
		final EditText et = (EditText) findViewById(R.id.editText1);
		et.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				// If the event is a key-down event on the "enter" button
				if ((arg2.getAction() == KeyEvent.ACTION_DOWN)
						&& (arg1 == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					UserTask myTask = new UserTask();
					myTask.execute(et.getText().toString());
					return true;
				}
				return false;
			}
		});

		// Sets up the refresh button
		Button refresh = (Button) findViewById(R.id.refresh);
		refresh.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					RequestTask myRequest = new RequestTask();
					myRequest.execute(latitude_.toString(),
							longitude_.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// Sets up the location manager
		LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1,
				this);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locManager.getBestProvider(crit, true);
		Location loc = locManager.getLastKnownLocation(provider);
		try {
			latitude_ = loc.getLatitude();
			longitude_ = loc.getLongitude();
			RequestTask myRequest = new RequestTask();
			myRequest.execute(latitude_.toString(), longitude_.toString());
		} catch (Exception e) {
			// Default coordinate just so the screen isn't empty
			// The default location is Blacksburg
			latitude_ = 37.2295733;
			longitude_ = -80.41393929999998;
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// This AsyncTask gets a latitude and longitude from a city and state
	class UserTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... cityState) {
			String stringResponse = "There was an error";
			try {
				// Formats the url then calls httpGet on the created url
				String url = "http://maps.googleapis.com/maps/api/geocode/json?address=";
				url += cityState[0].replace(" ", "+");
				url += "&sensor=true";
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(url);

				HttpResponse response;

				stringResponse = "There was an error";
				try {
					// Gets the http response and stores it as a url
					response = client.execute(request);
					stringResponse = RequestParse(EntityUtils.toString(response
							.getEntity()));
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return stringResponse;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result != "There was an error") {
				// Sends the retrieved latitude and longitude the corresponding
				// AsyncTask
				String[] listResponse;
				listResponse = result.split(",");
				RequestForecast task = new RequestForecast();
				task.execute(listResponse[0], listResponse[1]);
			}
		}

	}

	// This AsyncTask retrieves the http response that holds the forecast and
	// the weather for the current day
	class RequestForecast extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... longLat) {
			// Generates the appropriate url with the given parameters and calls
			// http get
			String url = "http://forecast.weather.gov/MapClick.php?";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("lat", longLat[0]));
			params.add(new BasicNameValuePair("lon", longLat[1]));
			String paramString = URLEncodedUtils.format(params, "utf-8");
			url += paramString;
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);

			HttpResponse response;
			String stringResponse = "There was an error";
			try {
				// Gets the response with the appropriate information and stores
				// it in a string
				response = client.execute(request);
				stringResponse = EntityUtils.toString(response.getEntity());
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return stringResponse;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			try {
				// Parses the result and outputs the result to the appropriate
				// text boxes
				UserParse(result);

				// Makes the tomorrow and today text boxes visible
				TextView todayTextView = (TextView) findViewById(R.id.todayText);
				TextView tomorrowTextView = (TextView) findViewById(R.id.tomorrowText);
				todayTextView.setVisibility(View.VISIBLE);
				tomorrowTextView.setVisibility(View.VISIBLE);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// This AsyncTask uses a latitude and longitude to get today's temperature
	// and wind speed
	class RequestTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... longLat) {
			// Generates the appropriate url from the input latitude and
			// longitude
			String url = "http://forecast.weather.gov/MapClick.php?";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("lat", longLat[0]));
			params.add(new BasicNameValuePair("lon", longLat[1]));
			String paramString = URLEncodedUtils.format(params, "utf-8");
			url += paramString;
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);

			HttpResponse response;
			String stringResponse = "There was an error";
			try {
				response = client.execute(request);
				stringResponse = EntityUtils.toString(response.getEntity());
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return stringResponse;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			// Parses the result and outputs the results to the appropriate text
			// boxes
			DefaultParse(result);
		}
	}

	// Parses the string and fills the appropriate text boxes
	public void DefaultParse(String webResponse) {
		try {
			String temperatureFarenheit = webResponse.substring(
					webResponse.indexOf("myforecast-current-lrg\">") + 24,
					webResponse.substring(
							webResponse.indexOf("myforecast-current-lrg\">"))
							.indexOf("&deg;F")
							+ webResponse.indexOf("myforecast-current-lrg\">"));

			String temperatureCelsius = webResponse.substring(webResponse
					.indexOf("myforecast-current-sm\">") + 23, webResponse
					.substring(webResponse.indexOf("myforecast-current-sm\">"))
					.indexOf("&deg;C")
					+ webResponse.indexOf("myforecast-current-sm\">"));

			String windSpeed = webResponse.substring(
					webResponse.indexOf("Wind Speed") + 17,
					webResponse.substring(webResponse.indexOf("Wind Speed"))
							.indexOf("</li>")
							+ webResponse.indexOf("Wind Speed"));

			TextView tempTextView = (TextView) findViewById(R.id.temperature);
			TextView windTextView = (TextView) findViewById(R.id.wind);

			// Changes the fonts and fills the text boxes
			tempTextView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC);
			windTextView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC);

			tempTextView.setText("Temperature: " + temperatureFarenheit
					+ " F, " + temperatureCelsius + " C");
			windTextView.setText("Wind: " + windSpeed);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Gets the latitude and longitude from the http response
	String RequestParse(String webResponse) {
		String lat = webResponse.substring(
				webResponse.indexOf("\"lat\" : ") + 8,
				webResponse.substring(webResponse.indexOf("\"lat\" : "))
						.indexOf(",") + webResponse.indexOf("\"lat\" : "));

		String lon = webResponse.substring(
				webResponse.indexOf("\"lng\" : ") + 8,
				webResponse.substring(webResponse.indexOf("\"lng\" : "))
						.indexOf("\n") + webResponse.indexOf("\"lng\" : "));
		return lat + "," + lon;

	}

	// Parses the response and retrieves today's and tomorrow's forecasts and
	// fills the appropriate text boxes
	void UserParse(String webResponse) {

		String temperatureFarenheit = webResponse.substring(webResponse
				.indexOf("myforecast-current-lrg\">") + 24, webResponse
				.substring(webResponse.indexOf("myforecast-current-lrg\">"))
				.indexOf("&deg;F")
				+ webResponse.indexOf("myforecast-current-lrg\">"));

		String temperatureCelsius = webResponse.substring(webResponse
				.indexOf("myforecast-current-sm\">") + 23, webResponse
				.substring(webResponse.indexOf("myforecast-current-sm\">"))
				.indexOf("&deg;C")
				+ webResponse.indexOf("myforecast-current-sm\">"));

		String weather = webResponse
				.substring(
						webResponse.indexOf("<p class=\"myforecast-current\">") + 30,
						webResponse
								.substring(
										webResponse
												.indexOf("<p class=\"myforecast-current\">"))
								.indexOf("</p>")
								+ webResponse
										.indexOf("<p class=\"myforecast-current\">"));

		String windSpeed = webResponse.substring(
				webResponse.indexOf("Wind Speed") + 17,
				webResponse.substring(webResponse.indexOf("Wind Speed"))
						.indexOf("</li>") + webResponse.indexOf("Wind Speed"));

		String stringToBeParsed = webResponse.substring(webResponse
				.indexOf("<p class=\"txt-ctr-caps\">") + 24);
		if (stringToBeParsed.substring(0, 25).toLowerCase().contains("night")) {
			stringToBeParsed = stringToBeParsed.substring(stringToBeParsed
					.indexOf("<p class=\"txt-ctr-caps\">") + 24);
		}

		String tomorrowTemp = stringToBeParsed
				.substring(
						stringToBeParsed.indexOf("High: ") + 6,
						stringToBeParsed.substring(
								stringToBeParsed.indexOf("High: ")).indexOf(
								"&deg;F")
								+ stringToBeParsed.indexOf("High: "));

		String tomorrowWeather = stringToBeParsed.substring(stringToBeParsed
				.indexOf("title=\"") + 7,
				stringToBeParsed
						.substring(stringToBeParsed.indexOf("title=\""))
						.indexOf("\" />")
						+ stringToBeParsed.indexOf("title=\""));

		TextView tempTextView = (TextView) findViewById(R.id.todayTemp);
		TextView windTextView = (TextView) findViewById(R.id.todayWind);
		TextView weatherTextView = (TextView) findViewById(R.id.todayWeather);
		TextView tomorrowTempTextView = (TextView) findViewById(R.id.tomorrowTemp);
		TextView tomorrowWeatherTextView = (TextView) findViewById(R.id.tomorrowWeather);

		// Sets the text box's fonts
		tempTextView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
		windTextView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
		weatherTextView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
		tomorrowTempTextView.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
		tomorrowWeatherTextView.setTypeface(Typeface.SANS_SERIF,
				Typeface.ITALIC);

		tempTextView.setText("Temperature: " + temperatureFarenheit + " F, "
				+ temperatureCelsius + " C");
		windTextView.setText("Wind: " + windSpeed);
		weatherTextView.setText("Weather: " + weather);
		tomorrowTempTextView.setText("High: " + tomorrowTemp);
		tomorrowWeatherTextView.setText(tomorrowWeather);
	}

	// Handles location changes
	@Override
	public void onLocationChanged(Location location) {
		Double lat = location.getLatitude();
		Double lon = location.getLongitude();
		latitude_ = lat;
		longitude_ = lon;
		RequestTask myLocationListener = new RequestTask();
		myLocationListener.execute(lat.toString(), lon.toString());
	}

	// Required override functions
	@Override
	public void onProviderDisabled(String provider) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}
}
