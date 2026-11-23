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

// Otomatik üretildi: tools/gen_encourage.py — elle değiştirmeyin.
/** Hedef tamamlanınca gösterilen (metin, kaynak) havuzu. */
val ENCOURAGE_ALL: List<Pair<String, String>> = listOf(
{rows}
)

/**
 * Havuzu karıştırıp tüketir; tüm havuz bitmeden aynı kayıt tekrar gelmez.
 * Havuz bitince yeniden karıştırır ve bir öncekiyle aynı kaydı başa koymaz.
 */
class EncouragePicker(private val pool: List<Pair<String, String>> = ENCOURAGE_ALL) {{
    private var deck = ArrayDeque<Pair<String, String>>()
    private var last: Pair<String, String>? = null

    fun next(): Pair<String, String> {{
        if (deck.isEmpty()) {{
            val fresh = pool.shuffled().toMutableList()
            if (fresh.size > 1 && fresh.first() == last) fresh.add(fresh.removeAt(0))
            deck = ArrayDeque(fresh)
        }}
        return deck.removeFirst().also {{ last = it }}
    }}
}}
'''
open(os.path.join(root, 'app/src/main/java/com/gezgin/zikirmatik/Encourage.kt'), 'w', encoding='utf-8').write(kotlin)
print("items", len(ITEMS))
