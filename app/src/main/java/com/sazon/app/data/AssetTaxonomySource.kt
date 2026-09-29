package com.sazon.app.data

import android.content.Context
import com.sazon.app.domain.taxonomy.Taxonomy
import com.sazon.app.domain.taxonomy.TaxonomyJson

class AssetTaxonomySource(private val context: Context) {

    fun load(): Taxonomy =
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            TaxonomyJson.parse(reader.readText())
        }

    private companion object {
        const val ASSET_NAME = "taxonomy.json"
    }
}
