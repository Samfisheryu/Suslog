package app.suslog.data.ai

import android.content.Context
import app.suslog.domain.tuning.BuiltInTuningDocuments
import app.suslog.domain.tuning.ai.TuningAiReferenceDocument
import app.suslog.domain.tuning.ai.TuningAiReferenceDocumentSource

class AssetBuiltInTuningDocumentRepository(
    context: Context,
) {
    private val assets = context.applicationContext.assets

    fun loadAll(): List<TuningAiReferenceDocument> =
        BuiltInTuningDocuments.all.map { document ->
            TuningAiReferenceDocument(
                id = document.id,
                title = document.title,
                source = TuningAiReferenceDocumentSource.BUILT_IN,
                content = assets.open(document.assetPath)
                    .bufferedReader()
                    .use { it.readText() }
            )
        }
}
