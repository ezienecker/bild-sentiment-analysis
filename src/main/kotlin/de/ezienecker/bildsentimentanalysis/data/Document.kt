package de.ezienecker.bildsentimentanalysis.data

/**
 * Document spiegelt die ID wieder um einen Artikel, Entitaet, Satz oder Wahl bzw. Umfragewert eindeutig zu identifizieren
 */
typealias Document = String

/**
 * Methode um den Datum-Teil des Strings zu extrahieren
 *
 * @return Gibt ein String zurueck der nur den Datum-Teil enthaelt
 */
fun Document.withoutArticle(): Document =
    this.substringBeforeLast("/")

/**
 * Methode um den Datum-Teil ohne Tag des Strings zu extrahieren
 *
 * @return Gibt ein String zurueck der nur den Datum-Teil ohne Tag enthaelt
 */
fun Document.withoutArticleAndDay(): Document =
    this.withoutArticle().substringBeforeLast("/")
