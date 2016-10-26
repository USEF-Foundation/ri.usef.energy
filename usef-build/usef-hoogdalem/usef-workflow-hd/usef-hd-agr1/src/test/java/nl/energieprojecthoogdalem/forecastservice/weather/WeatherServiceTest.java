/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.energieprojecthoogdalem.forecastservice.weather;

import info.usef.core.config.AbstractConfig;
import info.usef.core.util.PtuUtil;

import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Map;

import org.joda.time.LocalDate;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(WeatherService.class)
public class WeatherServiceTest
{
    private Properties prop;
    private WeatherService weatherService;

    private static int PTU_DURATION = 15;
    private static final double doubleOffest = 0.1D;
    private static final String invalidJson = "azerty", invalidjson2 = "{\"hello\": \"world\"}";
    private static final LocalDate period = new LocalDate(2016, 3, 11);

    private String weatherFile = AbstractConfig.getConfigurationFolder() + "weather.json";

    @Mock
    private URL url;

    @Mock
    private HttpURLConnection httpURLConnection;

    @Before
    public void init() throws Exception
    {
        prop = new Properties();

        PowerMockito.whenNew(URL.class).withArguments(Matchers.anyString()).thenReturn(url);

        PowerMockito.doReturn(httpURLConnection).when(url).openConnection();
        PowerMockito.when(httpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        PowerMockito.when(httpURLConnection.getResponseMessage()).thenReturn("OK");

        weatherService = new WeatherService(prop);
    }

    @Test
    public void testGetDayCorrectionURL() throws Exception
    {
        prop.setProperty("USE_WUNDERGROUND_FILE", "false");
        prop.setProperty("WUNDERGROUND_URL", "http://example.com");
        prop.setProperty("SKY_IRRADIATE", "0.9");

        PowerMockito.when(httpURLConnection.getInputStream()).thenReturn(new FileInputStream(weatherFile) );

        Map<Integer, Double> result = weatherService.getDayCorrection(period, PTU_DURATION);

        assertEquals(PtuUtil.getNumberOfPtusPerDay(period, PTU_DURATION), result.size());
    }

    @Test
    public void testGetDayCorrectionFile() throws Exception
    {
        prop.setProperty("USE_WUNDERGROUND_FILE", "true");

        FileInputStream fis = PowerMockito.spy(new FileInputStream(weatherFile));

        PowerMockito.whenNew(FileInputStream.class).withAnyArguments().thenReturn( fis );

        Map<Integer, Double> result = weatherService.getDayCorrection(period, PTU_DURATION);

        assertEquals(PtuUtil.getNumberOfPtusPerDay(period, PTU_DURATION), result.size());

    }

    //old forecast correction
    @Test
    public void testGetWeatherCorrection() throws Exception
    {
        weatherFile = AbstractConfig.getConfigurationFolder() + "old_weather.json";
        PowerMockito.when(httpURLConnection.getInputStream()).thenReturn(new FileInputStream(weatherFile) );

        double result = weatherService.getWeatherCorrection();
        assertEquals(0.3D, result, doubleOffest);
    }

    @Test
    public void testGetWeatherCorrectionErrorResponse() throws Exception
    {
        PowerMockito.when(httpURLConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_GATEWAY);
        PowerMockito.when(httpURLConnection.getResponseMessage()).thenReturn("BAD GATEWAY");

        double result = weatherService.getWeatherCorrection();
        assertEquals(0.2D, result, doubleOffest);
    }

    @Test
    public void testGetWeatherCorrectionInputUrlError() throws Exception
    {
        PowerMockito.whenNew(URL.class).withArguments(Matchers.anyString()).thenThrow(new MalformedURLException());
        double result = weatherService.getWeatherCorrection();
        assertEquals(0.2D, result, doubleOffest);
    }

    @Test
    public void testGetWeatherCorrectionInputJsonError() throws Exception
    {
        PowerMockito.when(httpURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(invalidJson.getBytes()));
        double result = weatherService.getWeatherCorrection();
        assertEquals(0.2D, result, doubleOffest);
    }

    @Test
    public void testGetWeatherCorrectionWrongInputJsonError() throws Exception
    {
        PowerMockito.when(httpURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(invalidjson2.getBytes()));
        double result = weatherService.getWeatherCorrection();
        assertEquals(0.2D, result, doubleOffest);
    }
}