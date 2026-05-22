package com.example.data

import android.content.Context
import org.json.JSONArray

data class BibleBook(
    val name: String,
    val chapters: Int,
    val isNewTestament: Boolean,
    val fileName: String // Add fileName for asset loading
)

object BibleData {
    val books = listOf(
        // Antiguo Testamento
        BibleBook("Génesis", 50, false, "genesis.json"),
        BibleBook("Éxodo", 40, false, "exodo.json"),
        BibleBook("Levítico", 27, false, "levitico.json"),
        BibleBook("Números", 36, false, "numeros.json"),
        BibleBook("Deuteronomio", 34, false, "deuteronomio.json"),
        BibleBook("Josué", 24, false, "josue.json"),
        BibleBook("Jueces", 21, false, "jueces.json"),
        BibleBook("Rut", 4, false, "rut.json"),
        BibleBook("1 Samuel", 31, false, "1_samuel.json"),
        BibleBook("2 Samuel", 24, false, "2_samuel.json"),
        BibleBook("1 Reyes", 22, false, "1_reyes.json"),
        BibleBook("2 Reyes", 25, false, "2_reyes.json"),
        BibleBook("1 Crónicas", 29, false, "1_cronicas.json"),
        BibleBook("2 Crónicas", 36, false, "2_cronicas.json"),
        BibleBook("Esdras", 10, false, "esdras.json"),
        BibleBook("Nehemías", 13, false, "nehemias.json"),
        BibleBook("Ester", 10, false, "ester.json"),
        BibleBook("Job", 42, false, "job.json"),
        BibleBook("Salmos", 150, false, "salmos.json"),
        BibleBook("Proverbios", 31, false, "proverbios.json"),
        BibleBook("Eclesiastés", 12, false, "eclesiastes.json"),
        BibleBook("Cantares", 8, false, "cantares.json"),
        BibleBook("Isaías", 66, false, "isaias.json"),
        BibleBook("Jeremías", 52, false, "jeremias.json"),
        BibleBook("Lamentaciones", 5, false, "lamentaciones.json"),
        BibleBook("Ezequiel", 48, false, "ezequiel.json"),
        BibleBook("Daniel", 12, false, "daniel.json"),
        BibleBook("Oseas", 14, false, "oseas.json"),
        BibleBook("Joel", 3, false, "joel.json"),
        BibleBook("Amós", 9, false, "amos.json"),
        BibleBook("Abdías", 1, false, "abdias.json"),
        BibleBook("Jonás", 4, false, "jonas.json"),
        BibleBook("Miqueas", 7, false, "miqueas.json"),
        BibleBook("Nahúm", 3, false, "nahum.json"),
        BibleBook("Habacuc", 3, false, "habacuc.json"),
        BibleBook("Sofonías", 3, false, "sofonias.json"),
        BibleBook("Hageo", 2, false, "hageo.json"),
        BibleBook("Zacarías", 14, false, "zacarias.json"),
        BibleBook("Malaquías", 4, false, "malaquias.json"),

        // Nuevo Testamento
        BibleBook("Mateo", 28, true, "mateo.json"),
        BibleBook("Marcos", 16, true, "marcos.json"),
        BibleBook("Lucas", 24, true, "lucas.json"),
        BibleBook("Juan", 21, true, "juan.json"),
        BibleBook("Hechos", 28, true, "hechos.json"),
        BibleBook("Romanos", 16, true, "romanos.json"),
        BibleBook("1 Corintios", 16, true, "1_corintios.json"),
        BibleBook("2 Corintios", 13, true, "2_corintios.json"),
        BibleBook("Gálatas", 6, true, "galatas.json"),
        BibleBook("Efesios", 6, true, "efesios.json"),
        BibleBook("Filipenses", 4, true, "filipenses.json"),
        BibleBook("Colosenses", 4, true, "colosenses.json"),
        BibleBook("1 Tesalonicenses", 5, true, "1_tesalonicenses.json"),
        BibleBook("2 Tesalonicenses", 3, true, "2_tesalonicenses.json"),
        BibleBook("1 Timoteo", 6, true, "1_timoteo.json"),
        BibleBook("2 Timoteo", 4, true, "2_timoteo.json"),
        BibleBook("Tito", 3, true, "tito.json"),
        BibleBook("Filemón", 1, true, "filemon.json"),
        BibleBook("Hebreos", 13, true, "hebreos.json"),
        BibleBook("Santiago", 5, true, "santiago.json"),
        BibleBook("1 Pedro", 5, true, "1_pedro.json"),
        BibleBook("2 Pedro", 3, true, "2_pedro.json"),
        BibleBook("1 Juan", 5, true, "1_juan.json"),
        BibleBook("2 Juan", 1, true, "2_juan.json"),
        BibleBook("3 Juan", 1, true, "3_juan.json"),
        BibleBook("Judas", 1, true, "judas.json"),
        BibleBook("Apocalipsis", 22, true, "apocalipsis.json")
    )

    fun getVerses(context: Context, bookName: String, chapter: Int): List<Pair<Int, String>> {
        val book = books.firstOrNull { it.name.equals(bookName, ignoreCase = true) } ?: return emptyList()
        return try {
            val jsonString = context.assets.open("biblia/${book.fileName}").bufferedReader().use { it.readText() }
            val chaptersArray = JSONArray(jsonString)
            if (chapter > 0 && chapter <= chaptersArray.length()) {
                val versesArray = chaptersArray.getJSONArray(chapter - 1)
                val verses = mutableListOf<Pair<Int, String>>()
                for (i in 0 until versesArray.length()) {
                    val verseText = versesArray.getString(i).replace("/n", "\n")
                    verses.add((i + 1) to verseText)
                }
                verses
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Helpers for Global Navigation
    val totalChapters: Int by lazy { books.sumOf { it.chapters } }

    fun getGlobalIndex(bookName: String, chapter: Int): Int {
        var index = 0
        for (book in books) {
            if (book.name.equals(bookName, ignoreCase = true)) {
                return index + (chapter - 1)
            }
            index += book.chapters
        }
        return 0
    }

    fun getBookAndChapterFromIndex(index: Int): Pair<BibleBook, Int> {
        var remaining = index
        for (book in books) {
            if (remaining < book.chapters) {
                return book to (remaining + 1)
            }
            remaining -= book.chapters
        }
        return books.last() to books.last().chapters
    }

    val inspirationalVerses = listOf(
        Triple("Salmos", "23:1-2", "Jehová es mi pastor; nada me faltará. En lugares de delicados pastos me hará descansar; junto a aguas de reposo me pastoreará."),
        Triple("Filipenses", "4:13", "Todo lo puedo en Cristo que me fortalece."),
        Triple("Juan", "3:16", "Porque de tal manera amó Dios al mundo, que ha dado a su Hijo unigénito, para que todo aquel que en él cree, no se pierda, mas tenga vida eterna."),
        Triple("Jeremías", "29:11", "Porque yo sé los pensamientos que tengo acerca de vosotros, dice Jehová, pensamientos de paz, y no de mal, para daros el fin que esperáis."),
        Triple("Josué", "1:9", "Mira que te mando que te esfuerces y seas valiente; no temas ni desmayes, porque Jehová tu Dios estará contigo en dondequiera que vayas."),
        Triple("Romanos", "8:28", "Y sabemos que a los que aman a Dios, todas las cosas les ayudan a bien, esto es, a los que conforme a su propósito son llamados."),
        Triple("Salmos", "46:1", "Dios es nuestro amparo y fortaleza, nuestro pronto auxilio en las tribulaciones."),
        Triple("Proverbios", "3:5-6", "Fíate de Jehová de todo tu corazón, y no te apoyes en tu propia prudencia. Reconócelo en todos tus caminos, y él enderezará tus veredas."),
        Triple("Isaías", "41:10", "No temas, porque yo estoy contigo; no desmayes, porque yo soy tu Dios que te esfuerzo; siempre te ayudaré, siempre te sustentaré con la diestra de mi justicia."),
        Triple("Mateo", "11:28", "Venid a mí todos los que estáis trabajados y cargados, y yo os haré descansar."),
        Triple("Salmos", "121:1-2", "Alzaré mis ojos a los montes; ¿De dónde vendrá mi auxilio? Mi auxilio viene de Jehová, que hizo los cielos y la tierra."),
        Triple("2 Timoteo", "1:7", "Porque no nos ha dado Dios espíritu de cobardía, sino de poder, de amor y de dominio propio."),
        Triple("Gálatas", "5:22-23", "Mas el fruto del Espíritu es amor, gozo, paz, paciencia, benignidad, bondad, fe, mansedumbre, templanza; contra tales cosas no hay ley."),
        Triple("Efesios", "2:8", "Porque por gracia sois salvos por medio de la fe; y esto no de vosotros, pues es don de Dios."),
        Triple("Lamentaciones", "3:22-23", "Por la misericordia de Jehová no hemos sido consumidos, porque nunca decayeron sus misericordias. Nuevas son cada mañana; grande es tu fidelidad."),
        Triple("Romanos", "12:2", "No os conforméis a este siglo, sino transformaos por medio de la renovación de vuestro entendimiento, para que comprobéis cuál sea la buena voluntad de Dios, agradable y perfecta."),
        Triple("Salmos", "37:4", "Deléitate asimismo en Jehová, y él te concederá las peticiones de tu corazón."),
        Triple("Salmos", "91:1-2", "El que habita al abrigo del Altísimo morará bajo la sombra del Omnipotente. Diré yo a Jehová: Esperanza mía, y castillo mío; mi Dios, en quien confiaré."),
        Triple("Mateo", "6:33", "Mas buscad primeramente el reino de Dios y su justicia, y todas estas cosas os serán añadidas."),
        Triple("Hebreos", "11:1", "Es, pues, la fe la certeza de lo que se espera, la convicción de lo que no se ve.")
    )
}
