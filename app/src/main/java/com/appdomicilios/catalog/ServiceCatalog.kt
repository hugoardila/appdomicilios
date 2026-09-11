package com.appdomicilios.catalog

import com.appdomicilios.model.OrderCategory

data class CatalogSubcategory(
    val id: String,
    val label: String,
    val suggestions: List<String>,
)

object ServiceCatalog {
    private val shoppingSubcategories = listOf(
        CatalogSubcategory(
            id = "supermercado",
            label = "Supermercado",
            suggestions = listOf(
                "Arroz",
                "Azucar",
                "Aceite",
                "Huevos",
                "Leche",
                "Pan tajado",
                "Cafe",
                "Pasta",
                "Atun",
                "Detergente",
                "Papel higienico",
                "Lentejas",
                "Frijoles",
                "Harina pan",
                "Mayonesa",
            ),
        ),
        CatalogSubcategory(
            id = "licores",
            label = "Licores",
            suggestions = listOf(
                "Cerveza Aguila",
                "Cerveza Poker",
                "Aguardiente",
                "Ron Viejo de Caldas",
                "Whisky",
                "Vodka",
                "Vino tinto",
                "Hielo",
                "Gaseosa 3 litros",
            ),
        ),
        CatalogSubcategory(
            id = "drogueria",
            label = "Drogueria",
            suggestions = listOf(
                "Acetaminofen",
                "Ibuprofeno",
                "Omeprazol",
                "Vitamina C",
                "Suero oral",
                "Alcohol",
                "Gasa",
                "Tapabocas",
                "Pañales",
                "Toallas higienicas",
                "Pañitos humedos",
            ),
        ),
        CatalogSubcategory(
            id = "fruver",
            label = "Frutas y verduras",
            suggestions = listOf(
                "Papa",
                "Tomate",
                "Cebolla larga",
                "Cebolla cabezona",
                "Platano",
                "Banano",
                "Limon",
                "Naranja",
                "Aguacate",
                "Cilantro",
                "Zanahoria",
                "Lechuga",
            ),
        ),
        CatalogSubcategory(
            id = "carniceria",
            label = "Carniceria",
            suggestions = listOf(
                "Carne molida",
                "Pechuga de pollo",
                "Muslos de pollo",
                "Costilla de cerdo",
                "Chorizo",
                "Carne para sudar",
                "Hueso carnudo",
            ),
        ),
        CatalogSubcategory(
            id = "ferreteria",
            label = "Ferreteria",
            suggestions = listOf(
                "Bombillo LED",
                "Cinta aislante",
                "Clavos",
                "Tornillos",
                "Candado",
                "Extension electrica",
                "Silicona",
                "Pegante PVC",
            ),
        ),
        CatalogSubcategory(
            id = "panaderia",
            label = "Panaderia",
            suggestions = listOf(
                "Pan frances",
                "Pan aliñado",
                "Roscones",
                "Arepas",
                "Torta",
                "Galletas",
                "Pastel de pollo",
            ),
        ),
    )

    private val tramitesSubcategories = listOf(
        CatalogSubcategory(
            id = "alcaldia",
            label = "Alcaldia",
            suggestions = listOf(
                "Impuesto predial",
                "Paz y salvo",
                "Certificado de residencia",
                "Sisbén",
                "Uso de suelo",
                "Industria y comercio",
            ),
        ),
        CatalogSubcategory(
            id = "citas_medicas",
            label = "Citas medicas",
            suggestions = listOf(
                "Asignar cita",
                "Reclamar autorizacion",
                "Radicar formula",
                "Reclamar medicamentos",
                "Pago de copago",
                "Reclamar resultados",
            ),
        ),
        CatalogSubcategory(
            id = "pagos",
            label = "Pagos",
            suggestions = listOf(
                "Recibo de energia",
                "Recibo de agua",
                "Recibo de gas",
                "Internet",
                "Recarga celular",
                "Giro",
                "Administracion",
                "Seguro",
            ),
        ),
        CatalogSubcategory(
            id = "notaria",
            label = "Notaria",
            suggestions = listOf(
                "Autenticacion",
                "Registro civil",
                "Declaracion extrajuicio",
                "Reconocimiento de firma",
                "Copia autentica",
            ),
        ),
        CatalogSubcategory(
            id = "bancarios",
            label = "Bancarios",
            suggestions = listOf(
                "Consignacion",
                "Retiro ventanilla",
                "Pago credito",
                "Extracto bancario",
                "Transferencia asistida",
            ),
        ),
        CatalogSubcategory(
            id = "impresiones",
            label = "Impresiones y copias",
            suggestions = listOf(
                "Fotocopias",
                "Impresion blanco y negro",
                "Impresion a color",
                "Anillado",
                "Laminado",
                "Escanear documento",
            ),
        ),
    )

    private val envioSubcategories = listOf(
        CatalogSubcategory(
            id = "sobres",
            label = "Sobres",
            suggestions = emptyList(),
        ),
        CatalogSubcategory(
            id = "documentos",
            label = "Documentos",
            suggestions = emptyList(),
        ),
        CatalogSubcategory(
            id = "paquetes",
            label = "Paquetes",
            suggestions = emptyList(),
        ),
        CatalogSubcategory(
            id = "cajas",
            label = "Cajas",
            suggestions = emptyList(),
        ),
        CatalogSubcategory(
            id = "fragil",
            label = "Fragil",
            suggestions = emptyList(),
        ),
    )

    fun subcategoriesFor(category: OrderCategory): List<CatalogSubcategory> {
        return when (category) {
            OrderCategory.SHOPPING -> shoppingSubcategories
            OrderCategory.TRAMITES -> tramitesSubcategories
            OrderCategory.ENVIOS -> envioSubcategories
            else -> emptyList()
        }
    }

    fun suggestionsFor(
        category: OrderCategory,
        subcategoryId: String?,
    ): List<String> {
        return subcategoriesFor(category)
            .firstOrNull { it.id == subcategoryId }
            ?.suggestions
            .orEmpty()
    }
}
