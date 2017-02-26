# RestazoClient v1b [José M. Carnero](http://sargazos.net)
===

## Cliente REST para Android.

Ésta librería permite realizar peticiones a servicios REST de forma asíncrona. Devuelve los datos recuperados (o los errores, si fuera el caso) como _hashmap_.

Si bien es funcional todavía está en fase beta; queda mucho por hacer.

- Peticiones asíncronas.
- Tanto la URL del servicio REST, como el método de petición y los parámetros son asignables.
- Desacoplada, realiza las acciones necesarias mediante _callback_.

*Ejemplo:*
```java
RestazoClient.RestazoClientCallback resCallback = new RestazoClient.RestazoClientCallback() {
    @Override
    public Boolean onCallback(Map<String, Object> aResult){
        TextView tvInfo = (TextView)findViewById(R.id.textView_info);

        String sText = "Cargando...";

        if (!aResult.isEmpty()) {
            String sStatus = (String) aResult.get("status");
            String  sError = (String) aResult.get("error");

            if (sError != null && !sError.isEmpty()) {
                tvInfo.setText("Error: " + sError);
                return false;
            }

            sText = sStatus;

            if (sStatus.equals("done")) {
                sText = "Finalizada recuperación";
                tvInfo.setVisibility(View.INVISIBLE);
            }

            tvInfo.setText(sText);
        }

        return true;
    }
};

new RestazoClient(resCallback).execute(sUrl, "post", "param1=0", "param2=5");
```

Si no se requieren parametros ni tipo de petición (GET por defecto) puede llamarse con:

```java
new RestazoClient(resCallback).execute(sUrl);
```

_Testeo en Android 4.4_.

====
**In English:**

## REST client for Android.

This library makes asynchronous REST requests. Returns data (or errors, if occurs) has a _hashmap_.

While functional it is still in beta stage; There is much to do.

- Asynchronous request.
- REST service URL, request method and request parameters are assigned on each petition.
- Decoupled, make the work with an user callback.

*Example:*
```java
RestazoClient.RestazoClientCallback resCallback = new RestazoClient.RestazoClientCallback() {
    @Override
    public Boolean onCallback(Map<String, Object> aResult){
        TextView tvInfo = (TextView)findViewById(R.id.textView_info);

        String sText = "Loading...";

        if (!aResult.isEmpty()) {
            String sStatus = (String) aResult.get("status");
            String  sError = (String) aResult.get("error");

            if (sError != null && !sError.isEmpty()) {
                tvInfo.setText("Error: " + sError);
                return false;
            }

            sText = sStatus;

            if (sStatus.equals("done")) {
                sText = "Request finished";
                tvInfo.setVisibility(View.INVISIBLE);
            }

            tvInfo.setText(sText);
        }

        return true;
    }
};

new RestazoClient(resCallback).execute(sUrl, "post", "param1=0", "param2=5");
```

If no parameters or type of request (GET by default) are required, it can be called with:

```java
new RestazoClient(resCallback).execute(sUrl);
```

_Tested on Android 4.4_.
