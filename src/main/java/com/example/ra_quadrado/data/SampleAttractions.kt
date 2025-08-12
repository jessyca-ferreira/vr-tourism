package com.example.ra_quadrado.data

import com.example.ra_quadrado.models.Attraction
import com.example.ra_quadrado.models.LatLng

object SampleAttractions {
    fun get(): List<Attraction> {
        return listOf(
            Attraction(
                id = "1",
                name = "Instituto Ricardo Brennand",
                description = "Um museu deslumbrante em estilo castelo que abriga uma das maiores coleções de arte brasileira e europeia da América Latina.",
                history = "Fundado em 2002 pelo empresário Ricardo Brennand, este complexo museológico apresenta um castelo em estilo medieval cercado por jardins. Abriga uma extensa coleção de arte brasileira e europeia, incluindo obras de Frans Post, o primeiro artista europeu a pintar paisagens brasileiras.",
                importance = "O Instituto Ricardo Brennand é uma das instituições culturais mais importantes do Brasil, mostrando o rico patrimônio artístico de Pernambuco e do Brasil. A própria arquitetura do castelo é uma obra de arte, criando uma experiência cultural única.",
                imageUrl = "https://www.taideturismo.com.br/wp-content/uploads/2019/07/brennand1.jpg",
                category = "Museu",
                rating = 4.8,
                visitDuration = 180,
                price = 30.0,
                location = LatLng(-8.0394, -34.9176),
                tags = listOf("Arte", "História", "Arquitetura", "Castelo"),
                address = "Alameda Antônio Brennand, s/n - Várzea, Recife - PE, 50741-904",
                phoneNumber = "+55 81 2121-0352",
                website = "https://www.institutoricardobrennand.org.br/",
                openingHours = mapOf(
                    "Segunda-feira" to "Fechado", "Terça-feira" to "13:00 - 17:00",
                    "Quarta-feira" to "13:00 - 17:00", "Quinta-feira" to "13:00 - 17:00",
                    "Sexta-feira" to "13:00 - 17:00", "Sábado" to "13:00 - 17:00", "Domingo" to "13:00 - 17:00"
                )
            ),
            Attraction(
                id = "2",
                name = "Museu Cais do Sertão",
                description = "Um museu interativo dedicado à cultura e história do Nordeste brasileiro, especialmente da região do sertão.",
                history = "Inaugurado em 2014, o museu está localizado na antiga área portuária do Recife e celebra a cultura do Nordeste brasileiro através de exposições interativas, música e instalações multimídia.",
                importance = "O museu preserva e promove o rico patrimônio cultural da região Nordeste, incluindo o legado de Luiz Gonzaga, o \"Rei do Baião\", e mostra a resiliência e criatividade do povo sertanejo.",
                imageUrl = "https://images.adsttc.com/media/images/5c11/986d/08a5/e54b/ad00/0946/newsletter/5MG_4603.jpg?1544656958",
                category = "Museu",
                rating = 4.6,
                visitDuration = 120,
                price = 10.0,
                location = LatLng(-8.0594, -34.8716),
                tags = listOf("Cultura", "Interativo", "Música", "História"),
                address = "Armazém 10, Av. Alfredo Lisboa, s/n - Recife Antigo, Recife - PE, 50030-150",
                phoneNumber = "+55 81 3182-8268",
                website = "https://www.museucaisdosertao.pe.gov.br/",
                openingHours = mapOf(
                    "Segunda-feira" to "Fechado", "Terça-feira" to "09:00 - 17:00",
                    "Quarta-feira" to "09:00 - 17:00", "Quinta-feira" to "09:00 - 17:00",
                    "Sexta-feira" to "09:00 - 17:00", "Sábado" to "09:00 - 17:00", "Domingo" to "09:00 - 17:00"
                )
            ),
            Attraction(
                id = "3",
                name = "Paço do Frevo",
                description = "Um centro cultural dedicado ao frevo, o enérgico estilo de dança e música que é a alma do Carnaval do Recife.",
                history = "Inaugurado em 2014, o Paço do Frevo está localizado em um edifício histórico restaurado e serve como museu vivo e centro cultural para o frevo, que foi declarado Patrimônio Cultural Imaterial da Humanidade pela UNESCO.",
                importance = "O frevo é a expressão cultural mais importante de Pernambuco, representando a alegria, criatividade e resiliência do povo do Recife. O centro preserva esta tradição e educa os visitantes sobre sua importância.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/0c/Fachada_Paco_do_Frevo_mai_2025.jpg",
                category = "Centro Cultural",
                rating = 4.5,
                visitDuration = 90,
                price = 8.0,
                location = LatLng(-8.0614, -34.8716),
                tags = listOf("Frevo", "Dança", "Música", "Carnaval"),
                address = "Praça do Arsenal da Marinha, s/n - Recife Antigo, Recife - PE, 50030-000",
                phoneNumber = "+55 81 3355-9500",
                website = "https://pacodofrevo.org.br/",
                openingHours = mapOf(
                    "Segunda-feira" to "Fechado", "Terça-feira" to "09:00 - 17:00",
                    "Quarta-feira" to "09:00 - 17:00", "Quinta-feira" to "09:00 - 17:00",
                    "Sexta-feira" to "09:00 - 17:00", "Sábado" to "09:00 - 17:00", "Domingo" to "09:00 - 17:00"
                )
            )
        )
    }
}