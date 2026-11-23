"""encourage_items.py'den web/encourage.js ve Android Encourage.kt üretir."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from encourage_items import ITEMS

root = os.path.join(os.path.dirname(__file__), '..')
js = "// Otomatik üretildi: tools/gen_encourage.py\nwindow.ENCOURAGE = " + json.dumps(
    [{"t": t, "s": s} for t, s in ITEMS], ensure_ascii=False, indent=1) + ";\n"
open(os.path.join(root, 'web', 'encourage.js'), 'w', encoding='utf-8').write(js)

def kt(x):
    return '"' + x.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$') + '"'
rows = ",\n".join(f"    {kt(t)} to {kt(s)}" for t, s in ITEMS)
kotlin = f'''package com.gezgin.zikirmatik

import android.content.SharedPreferences

// Otomatik üretildi: tools/gen_encourage.py — elle değiştirmeyin.
/** Hedef tamamlanınca gösterilen (metin, kaynak) havuzu. */
val ENCOURAGE_ALL: List<Pair<String, String>> = listOf(
{rows}
)

/**
 * Havuzu karıştırıp tüketir; tüm havuz bitmeden aynı kayıt tekrar gelmez.
 * Sıra ve kalan kayıtlar SharedPreferences'ta tutulur: uygulama yeniden açılsa da devam eder.
 * Havuz bitince yeniden karıştırır; önceki kayıt başa gelmez.
 */
class EncouragePicker(
    private val prefs: SharedPreferences,
    private val pool: List<Pair<String, String>> = ENCOURAGE_ALL,
) {{
    private val kDeck = "enc_deck"
    private val kLast = "enc_last"

    private fun load(): MutableList<Int> =
        (prefs.getString(kDeck, "") ?: "").split(',').mapNotNull {{ it.toIntOrNull() }}
            .filter {{ it in pool.indices }}.toMutableList()

    private fun save(deck: List<Int>, last: Int) {{
        prefs.edit().putString(kDeck, deck.joinToString(",")).putInt(kLast, last).apply()
    }}

    fun next(): Pair<String, String> {{
        if (pool.isEmpty()) return "" to ""
        val deck = load()
        val last = prefs.getInt(kLast, -1)
        if (deck.isEmpty()) {{
            deck.addAll(pool.indices.shuffled())
            if (deck.size > 1 && deck.first() == last) deck.add(deck.removeAt(0))
        }}
        val idx = deck.removeAt(0)
        save(deck, idx)
        return pool[idx]
    }}
}}
'''
open(os.path.join(root, 'app/src/main/java/com/gezgin/zikirmatik/Encourage.kt'), 'w', encoding='utf-8').write(kotlin)
print("items", len(ITEMS))
