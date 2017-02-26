/*
Copyright 2017 José M. Carnero <jm_carnero@sargazos.net>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package net.sargazos;

import android.net.SSLCertificateSocketFactory;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Cliente para servicios Rest
 *
 * Indicadores de progreso, bajo la clave "status":
 * 1 -> ini (onPreExecute),
 * 2 -> url (doInBackground),
 * 3 -> map (doInBackground),
 * 4 -> end (doInBackground),
 * 5 -> done (onPostExecute)
 *
 * ej: new RestClient(callback).execute(sUrl);
 *
 * @author jmanuel
 * @since 2017-01-25
 * @version 1b
 * @todo concretar los mensajes de salida segun excepcion generada
 */
public class RestazoClient extends AsyncTask<String, String, Map<String, Object>> {

    private String sCharset = "UTF-8"; //juego de caracteres que se usara para url's y demas, de momento forzado

    private boolean bDone = false; //indica si ya se ha realizado la tarea (true)

    private RestazoClientCallback callback = null; //se muestra progreso, mensajes y resultado

    private String sUrl; //url a la que se hara la peticion rest
    private String sMethod; //metodo HTTP para llamar a la URL: GET, POST, PUT, ...
    private String sParams = ""; //cadena de parametros HTTP (si los hubiera)

    /**
     * Permite pasar funciones callback a ejecutar cuando se recuperen los datos
     */
    public interface RestazoClientCallback {
        Boolean onCallback(Map<String, Object> sResult);
    }

    @Override
    protected Map<String, Object> doInBackground(String... aParams) {
        this.sUrl = aParams[0];
        this.sMethod = (aParams.length < 2) ? "GET" : aParams[1].toUpperCase();
        this.httpParams(aParams);

        Map<String, Object> aReturn = null;
        JSONObject oJson = new JSONObject();

        try { //recuperar informacion de la url
            this.publishProgress("url");

            String responseBody = this.getUrl();

            try { //convertir la informacion recuperada a hashmap //TODO opcion de recuperar XML
                this.publishProgress("map");

                oJson = this.getJson(responseBody);
                aReturn = this.jsonToMap(oJson);
            } catch (Exception e) {
                //e.printStackTrace();
                aReturn.put("error", e.getLocalizedMessage());
            }
        } catch (Exception e) {
            //e.printStackTrace();
            aReturn.put("error", e.getLocalizedMessage());
        }

        this.bDone = true;
        this.publishProgress("end");

        return aReturn;
    }

    /**
     * Procesa el JSON recogido en RestClient::getUrl()
     *
     * @todo concretar los mensajes de salida segun excepcion generada
     * @param sResponseBody
     * @return
     */
    private JSONObject getJson(String sResponseBody) throws Exception {

        JSONObject oJson = new JSONObject(sResponseBody);

        return oJson;
    }

    /**
     * Abre la conexion con la url solicitada y recupera su contenido
     *
     * @todo concretar los mensajes de salida segun excepcion generada
     * @return Devuelve el contenido leido de la URL solicitada
     */
    private String getUrl() throws Exception {

        String sReturn = new String();

        if (this.sMethod.equals("GET") && this.sParams.length() > 0) { //TODO ver que hacer con otros tipos de peticion o si es esta la forma mas correcta de añadir los parametros a la url
            this.sUrl += "&" + this.sParams; //TODO comprobar si es necesario poner un ?
        }

        URL oUrl = new URL(this.sUrl); //Create URL

        HttpURLConnection myConnection = (HttpURLConnection) oUrl.openConnection(); //Create connection

        //FIXME solo para pruebas, deshabilita comprobacion SSL
        /*if (myConnection instanceof HttpsURLConnection) {
            HttpsURLConnection myHttpsConnection = (HttpsURLConnection) myConnection;
            myHttpsConnection.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));

            //myHttpsConnection.setHostnameVerifier(new AllowAllHostnameVerifier());

            //crea un HostnameVerifier que suplanta al esperado
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    return hv.verify("itaca.local", session);
                    //return true;
                }
            };

            myHttpsConnection.setHostnameVerifier(hostnameVerifier);
        }/**/

        myConnection.setRequestMethod(this.sMethod);
        this.sMethod = myConnection.getRequestMethod(); //por si no se ha pasado un metodo correcto //TODO posible problema si los parametros ya se han añadido al ser GET

        myConnection.setRequestProperty("User-Agent", "sargazos.net-RestazoClient-v1b");
        myConnection.setRequestProperty("Accept-Charset", this.sCharset);
        myConnection.setRequestProperty("Accept", "application/json"); //de momento espera json, forzado
        //myConnection.setRequestProperty("ACCEPT-LANGUAGE", "en-US,en;0.5");

        if (this.sMethod.equals("POST") || this.sMethod.equals("PUT")) { //TODO ver que otros tipos de peticion requieren Content-Type
            myConnection.setDoOutput(true); //fuerza el cambio del tipo de petición a POST si encuentra GET
            myConnection.setRequestProperty("Content-Type", "application/x-ww-form-urlencoded");

            if (this.sParams.length() > 0) { //parametros HTTP
                myConnection.setRequestProperty("Content-Length", "" + Integer.toString(this.sParams.getBytes().length));
                myConnection.setFixedLengthStreamingMode(this.sParams.getBytes().length);
                PrintWriter out = new PrintWriter(myConnection.getOutputStream());
                out.print(this.sParams);
                out.close();
            }
        }

        if (myConnection.getResponseCode() == 200) {
            InputStream responseBody = myConnection.getInputStream();

            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            BufferedReader bufferRead = new BufferedReader(new InputStreamReader(responseBody, this.sCharset));
            while ((line = bufferRead.readLine()) != null) {
                stringBuilder.append(line);
            }

            sReturn = stringBuilder.toString();
        } else {
            sReturn = "{\"error\":\"HTTP Connection code: " + myConnection.getResponseCode() + "\"}";
        }

        myConnection.disconnect(); //cerrando conexion

        return sReturn;
    }

    /**
     * Prepara los parametros, para la posterior peticion HTTP
     *
     * @param aParams Parametros recibidos, el primero es la URL de peticion, el segundo puede ser (si no, se toma GET por defecto) el método de la peticion, el resto (si hay) los parametros HTTP
     */
    private void httpParams(String[] aParams) {
        ArrayList<String> aParamsTemp = new ArrayList<String>();

        if (aParams.length > 1) {
            //System.arraycopy(aParams, 0, this.aParams, 1, aParams.length - 1);

            for (int iCont = 1; iCont < aParams.length; iCont++) {
                Integer iIgual = aParams[iCont].indexOf('=');

                if (iIgual != -1) { //solo los fragmentos "clave=valor"
                    try {
                        String sIni = URLEncoder.encode(aParams[iCont].substring(0, iIgual), this.sCharset); //TODO debe url-codificarse esta parte?
                        String sFin = URLEncoder.encode(aParams[iCont].substring(iIgual + 1), this.sCharset); //TODO debe url-codificarse esta parte?

                        aParamsTemp.add(sIni + "=" + sFin);
                    } catch (UnsupportedEncodingException e) { //problemas con el charset
                        //e.printStackTrace();
                        aParamsTemp.add("-=-");
                    }
                }
            }
        }

        this.sParams = this.implode(aParamsTemp, "&");
    }

    /**
     * Convierte un array de strings en una cadena concatenando cada elemento del array con el separador deseado
     *
     * @param aFragmentos
     * @param sSeparador
     * @return String
     */
    private String implode(ArrayList<String> aFragmentos, String sSeparador) {
        if(aFragmentos.size() == 0){
            return "";
        }

        StringBuilder sbOut = new StringBuilder("");

        for (int i = 0; i < aFragmentos.size(); i++) {
            if (i != 0) {
                sbOut.append(sSeparador);
            }
            sbOut.append(aFragmentos.get(i));
        }

        return sbOut.toString();
    }

    /**
     * Devuelve si ya se a realizado la tarea
     *
     * @return boolean
     */
    public boolean isDone() {
        return bDone;
    }

    @Override
    protected void onPostExecute(Map<String, Object> results) {
        super.onPostExecute(results);

        results.put("status", "done");

        if( !isCancelled() ) {
            this.callback.onCallback(results);
            return;
        }

        //mProgress.setVisibility(View.GONE);
        //Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Map<String, Object> aMap = new HashMap<String, Object>(){{ put("status", "ini"); }};

        this.callback.onCallback(aMap);
    }

    @Override
    protected void onProgressUpdate(String... sProgress) {
        super.onProgressUpdate(sProgress);

        Map<String, Object> aMap = new HashMap<String, Object>();
        aMap.put("status", sProgress[0]);

        this.callback.onCallback(aMap);
    }

    /**
     * Constructor
     */
    public RestazoClient(RestazoClientCallback callback){
        //this.sUrl = sUrl;
        this.callback = callback;
    }

    /**
     * Convierte un objeto JSONObject a un hashmap
     *
     * @author Vikas Gupta <http://stackoverflow.com/users/2915208/vikas-gupta>
     * @throws JSONException
     * @param json
     * @return
     */
    public Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if(json != JSONObject.NULL) {
            retMap = this.toMap(json);
        }

        return retMap;
    }

    /**
     * @author Vikas Gupta <http://stackoverflow.com/users/2915208/vikas-gupta>
     * @throws JSONException
     * @param object
     * @return
     */
    public Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = this.toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = this.toMap((JSONObject) value);
            }
            map.put(key, value);
        }

        return map;
    }

    /**
     * @author Vikas Gupta <http://stackoverflow.com/users/2915208/vikas-gupta>
     * @throws JSONException
     * @param array
     * @return
     */
    public List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();

        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = this.toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = this.toMap((JSONObject) value);
            }
            list.add(value);
        }

        return list;
    }
}
