package com.luis.gvlauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Destino favorito de la pantalla Navegación. */
data class Place(val name: String, val address: String)

/** Contacto de marcación rápida de la pantalla Teléfono. */
data class Contact(val name: String, val number: String)

/** Guarda destinos, contactos y las direcciones de Casa/Trabajo (para Google Maps). */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("favoritos", Context.MODE_PRIVATE)

    fun places(): List<Place> = read("lugares") { Place(it.optString("n"), it.optString("d")) }
    fun savePlaces(list: List<Place>) = write("lugares", list) { JSONObject().put("n", it.name).put("d", it.address) }

    fun contacts(): List<Contact> = read("contactos") { Contact(it.optString("n"), it.optString("t")) }
    fun saveContacts(list: List<Contact>) = write("contactos", list) { JSONObject().put("n", it.name).put("t", it.number) }

    var homeAddress: String
        get() = prefs.getString("casa", "") ?: ""
        set(v) = prefs.edit().putString("casa", v.trim()).apply()

    var workAddress: String
        get() = prefs.getString("trabajo", "") ?: ""
        set(v) = prefs.edit().putString("trabajo", v.trim()).apply()

    private fun <T> read(key: String, parse: (JSONObject) -> T): List<T> = runCatching {
        val arr = JSONArray(prefs.getString(key, null) ?: "[]")
        (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun <T> write(key: String, list: List<T>, toJson: (T) -> JSONObject) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
