package com.finance;

import java.io.*;
import java.net.*;
import java.util.*;

public class CurrencyService {
    private static final String API_URL = "https://api.frankfurter.app/latest?from=";
    private static final Map<String, Double> rateCache = new HashMap<>();
    private static String cachedBase = "";
    private static long lastFetchTime = 0;
    private static final long CACHE_DURATION = 10 * 60 * 1000;

    public static final String[][] CURRENCIES = {
        {"BDT","BDT — Bangladeshi Taka"},{"USD","USD — US Dollar"},{"EUR","EUR — Euro"},
        {"GBP","GBP — British Pound"},{"JPY","JPY — Japanese Yen"},{"CNY","CNY — Chinese Yuan"},
        {"INR","INR — Indian Rupee"},{"AUD","AUD — Australian Dollar"},{"CAD","CAD — Canadian Dollar"},
        {"CHF","CHF — Swiss Franc"},{"SGD","SGD — Singapore Dollar"},{"MYR","MYR — Malaysian Ringgit"},
        {"AED","AED — UAE Dirham"},{"SAR","SAR — Saudi Riyal"},{"KRW","KRW — South Korean Won"},
        {"THB","THB — Thai Baht"},{"HKD","HKD — Hong Kong Dollar"},{"NZD","NZD — New Zealand Dollar"},
        {"SEK","SEK — Swedish Krona"},{"NOK","NOK — Norwegian Krone"},{"TRY","TRY — Turkish Lira"},
        {"PKR","PKR — Pakistani Rupee"},{"IDR","IDR — Indonesian Rupiah"},{"PHP","PHP — Philippine Peso"},
        {"MXN","MXN — Mexican Peso"},{"BRL","BRL — Brazilian Real"},{"ZAR","ZAR — South African Rand"},
        {"EGP","EGP — Egyptian Pound"},{"KWD","KWD — Kuwaiti Dinar"},{"QAR","QAR — Qatari Riyal"},
    };

    public static String[] getCurrencyCodes() {
        return Arrays.stream(CURRENCIES).map(c -> c[0]).toArray(String[]::new);
    }
    public static String[] getCurrencyDisplayNames() {
        return Arrays.stream(CURRENCIES).map(c -> c[1]).toArray(String[]::new);
    }

    public static double convert(double amount, String from, String to) {
        if (from.equals(to)) return amount;
        Map<String, Double> rates = getRates(to);
        if (rates != null && rates.containsKey(from)) return amount / rates.get(from);
        rates = getRates(from);
        if (rates != null && rates.containsKey(to)) return amount * rates.get(to);
        return -1;
    }

    public static Map<String, Double> getRates(String base) {
        long now = System.currentTimeMillis();
        if (base.equals(cachedBase) && (now - lastFetchTime) < CACHE_DURATION && !rateCache.isEmpty())
            return new HashMap<>(rateCache);
        String fetchBase = base.equals("BDT") ? "USD" : base;
        try {
            URL url = new URL(API_URL + fetchBase);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            Map<String, Double> parsed = parseRates(sb.toString());
            if (base.equals("BDT")) {
                double bdtPerUsd = 110.0;
                Map<String, Double> result = new HashMap<>();
                for (Map.Entry<String, Double> e : parsed.entrySet())
                    result.put(e.getKey(), e.getValue() / bdtPerUsd);
                result.put("USD", 1.0 / bdtPerUsd);
                rateCache.clear(); rateCache.putAll(result);
            } else {
                rateCache.clear(); rateCache.putAll(parsed);
            }
            cachedBase = base; lastFetchTime = now;
            return new HashMap<>(rateCache);
        } catch (Exception e) { return null; }
    }

    public static String getRateDisplay(String from, String to) {
        if (from.equals(to)) return "1 " + from + " = 1 " + to;
        double c = convert(1.0, from, to);
        return c < 0 ? "Rate unavailable" : String.format("1 %s = %.4f %s", from, c, to);
    }

    private static Map<String, Double> parseRates(String json) {
        Map<String, Double> rates = new HashMap<>();
        int s = json.indexOf("\"rates\":"); if (s == -1) return rates;
        int o = json.indexOf("{", s), c = json.indexOf("}", o);
        for (String pair : json.substring(o+1, c).split(",")) {
            String[] kv = pair.trim().split(":");
            if (kv.length == 2) try {
                rates.put(kv[0].trim().replace("\"",""), Double.parseDouble(kv[1].trim()));
            } catch (NumberFormatException ignored) {}
        }
        return rates;
    }
}
