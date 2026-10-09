package br.com.caderno.parcial.dados

import br.com.caderno.parcial.modelo.Categoria
import br.com.caderno.parcial.modelo.Dificuldade
import br.com.caderno.parcial.modelo.Ingrediente
import br.com.caderno.parcial.modelo.Receita

/** Dados simulados (mocks): nesta etapa não há API nem banco de dados. */
object ReceitasMock {

    val receitas: List<Receita> = listOf(
        Receita(
            id = "brigadeiro",
            nome = "Brigadeiro",
            categoria = Categoria.DOCE,
            emoji = "🍫",
            tempoPreparoMin = 20,
            dificuldade = Dificuldade.FACIL,
            rendimento = 20,
            unidadeRendimento = "unidades",
            ingredientes = listOf(
                Ingrediente("leite condensado", 1.0, "lata"),
                Ingrediente("chocolate em pó", 2.0, "colher de sopa"),
                Ingrediente("manteiga", 1.0, "colher de sopa"),
                Ingrediente("Chocolate granulado", null, null),
            ),
            passos = listOf(
                "Em uma panela, junte o leite condensado, o chocolate e a manteiga.",
                "Mexa em fogo baixo até desgrudar do fundo da panela (uns 10 minutos).",
                "Deixe esfriar, unte as mãos com manteiga e enrole as bolinhas.",
                "Passe no granulado e coloque em forminhas.",
            ),
            dica = "O ponto certo é quando, ao inclinar a panela, a massa escorrega inteira.",
        ),
        Receita(
            id = "pao-de-queijo",
            nome = "Pão de queijo",
            categoria = Categoria.SALGADO,
            emoji = "🧀",
            tempoPreparoMin = 45,
            dificuldade = Dificuldade.MEDIA,
            rendimento = 30,
            unidadeRendimento = "unidades",
            ingredientes = listOf(
                Ingrediente("polvilho azedo", 500.0, "g"),
                Ingrediente("leite", 1.0, "xícara"),
                Ingrediente("óleo", 0.5, "xícara"),
                Ingrediente("ovos", 2.0, null),
                Ingrediente("queijo meia cura ralado", 250.0, "g"),
                Ingrediente("Sal", null, null),
            ),
            passos = listOf(
                "Ferva o leite com o óleo e o sal.",
                "Escalde o polvilho com essa mistura e mexa bem.",
                "Quando amornar, junte os ovos um a um e depois o queijo.",
                "Faça bolinhas e asse a 180 °C por cerca de 25 minutos.",
            ),
        ),
        Receita(
            id = "bolo-de-cenoura",
            nome = "Bolo de cenoura",
            categoria = Categoria.DOCE,
            emoji = "🥕",
            tempoPreparoMin = 60,
            dificuldade = Dificuldade.FACIL,
            rendimento = 12,
            unidadeRendimento = "fatias",
            ingredientes = listOf(
                Ingrediente("cenouras médias", 3.0, null),
                Ingrediente("ovos", 4.0, null),
                Ingrediente("óleo", 1.0, "xícara"),
                Ingrediente("açúcar", 2.0, "xícara"),
                Ingrediente("farinha de trigo", 2.5, "xícara"),
                Ingrediente("fermento em pó", 1.0, "colher de sopa"),
            ),
            passos = listOf(
                "Bata no liquidificador as cenouras, os ovos e o óleo.",
                "Despeje em uma tigela e misture o açúcar e a farinha.",
                "Por último, misture o fermento delicadamente.",
                "Asse em forma untada a 180 °C por 40 minutos.",
            ),
            dica = "Cobertura: 1 colher de manteiga, 3 de chocolate, 1 xícara de açúcar e 1 de leite.",
        ),
        Receita(
            id = "feijoada",
            nome = "Feijoada",
            categoria = Categoria.PRATO_PRINCIPAL,
            emoji = "🍲",
            tempoPreparoMin = 180,
            dificuldade = Dificuldade.DIFICIL,
            rendimento = 8,
            unidadeRendimento = "porções",
            ingredientes = listOf(
                Ingrediente("feijão preto", 1.0, "kg"),
                Ingrediente("carnes salgadas dessalgadas", 1.5, "kg"),
                Ingrediente("linguiças calabresas", 2.0, null),
                Ingrediente("cebolas", 2.0, null),
                Ingrediente("dentes de alho", 6.0, null),
                Ingrediente("Folhas de louro", null, null),
            ),
            passos = listOf(
                "Deixe o feijão de molho na noite anterior.",
                "Cozinhe o feijão com as carnes e o louro na pressão por 40 minutos.",
                "Refogue a cebola, o alho e a linguiça e junte ao feijão.",
                "Deixe apurar em fogo baixo até o caldo engrossar.",
            ),
            dica = "Sirva com arroz, couve refogada, farofa e laranja.",
        ),
        Receita(
            id = "moqueca",
            nome = "Moqueca de peixe",
            categoria = Categoria.PRATO_PRINCIPAL,
            emoji = "🐟",
            tempoPreparoMin = 50,
            dificuldade = Dificuldade.MEDIA,
            rendimento = 4,
            unidadeRendimento = "porções",
            ingredientes = listOf(
                Ingrediente("postas de peixe", 1.0, "kg"),
                Ingrediente("tomates", 3.0, null),
                Ingrediente("pimentões coloridos", 2.0, null),
                Ingrediente("leite de coco", 200.0, "ml"),
                Ingrediente("azeite de dendê", 2.0, "colher de sopa"),
                Ingrediente("Coentro", null, null),
            ),
            passos = listOf(
                "Tempere o peixe com limão e sal.",
                "Monte camadas de cebola, tomate, pimentão e peixe na panela.",
                "Regue com o leite de coco e o dendê e cozinhe tampado por 20 minutos.",
                "Finalize com coentro picado.",
            ),
        ),
        Receita(
            id = "coxinha",
            nome = "Coxinha",
            categoria = Categoria.SALGADO,
            emoji = "🍗",
            tempoPreparoMin = 90,
            dificuldade = Dificuldade.DIFICIL,
            rendimento = 25,
            unidadeRendimento = "unidades",
            ingredientes = listOf(
                Ingrediente("frango cozido e desfiado", 500.0, "g"),
                Ingrediente("caldo do cozimento", 2.0, "xícara"),
                Ingrediente("farinha de trigo", 2.0, "xícara"),
                Ingrediente("requeijão", 3.0, "colher de sopa"),
                Ingrediente("Farinha de rosca", null, null),
            ),
            passos = listOf(
                "Ferva o caldo e despeje a farinha de uma vez, mexendo até soltar.",
                "Sove a massa ainda morna até ficar lisa.",
                "Recheie com o frango e o requeijão e modele as coxinhas.",
                "Empane na farinha de rosca e frite em óleo quente.",
            ),
        ),
        Receita(
            id = "pudim",
            nome = "Pudim de leite",
            categoria = Categoria.DOCE,
            emoji = "🍮",
            tempoPreparoMin = 70,
            dificuldade = Dificuldade.MEDIA,
            rendimento = 10,
            unidadeRendimento = "fatias",
            ingredientes = listOf(
                Ingrediente("leite condensado", 1.0, "lata"),
                Ingrediente("leite (a mesma medida)", 1.0, "lata"),
                Ingrediente("ovos", 3.0, null),
                Ingrediente("açúcar para a calda", 1.0, "xícara"),
            ),
            passos = listOf(
                "Derreta o açúcar na forma até virar calda e espalhe.",
                "Bata o leite condensado, o leite e os ovos no liquidificador.",
                "Despeje na forma e asse em banho-maria a 180 °C por 1 hora.",
                "Leve à geladeira por 4 horas antes de desenformar.",
            ),
        ),
        Receita(
            id = "farofa",
            nome = "Farofa de bacon",
            categoria = Categoria.ACOMPANHAMENTO,
            emoji = "🥓",
            tempoPreparoMin = null, // de propósito: mostra o tratamento de valor ausente
            dificuldade = Dificuldade.FACIL,
            rendimento = 6,
            unidadeRendimento = "porções",
            ingredientes = listOf(
                Ingrediente("bacon em cubos", 200.0, "g"),
                Ingrediente("cebola", 1.0, null),
                Ingrediente("farinha de mandioca", 2.0, "xícara"),
                Ingrediente("manteiga", 2.0, "colher de sopa"),
                Ingrediente("Cheiro-verde", null, null),
            ),
            passos = listOf(
                "Frite o bacon até dourar.",
                "Junte a manteiga e a cebola e refogue.",
                "Acrescente a farinha aos poucos, mexendo até tostar.",
                "Finalize com cheiro-verde.",
            ),
        ),
    )

    /** Retorna `null` se o id não existir (ou vier nulo da Intent). */
    fun buscar(id: String?): Receita? = receitas.firstOrNull { it.id == id }
}
