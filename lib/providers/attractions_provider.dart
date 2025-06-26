import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:tourist_guide_app/models/attraction.dart';
import 'dart:math' as math;

class AttractionsProvider with ChangeNotifier {
  List<Attraction> _attractions = [];
  List<Attraction> _filteredAttractions = [];
  bool _isLoading = false;
  String? _errorMessage;
  String _selectedCategory = 'Todos';
  String _searchQuery = '';

  List<Attraction> get attractions => _attractions;
  List<Attraction> get filteredAttractions => _filteredAttractions;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String get selectedCategory => _selectedCategory;
  String get searchQuery => _searchQuery;

  List<String> get categories {
    Set<String> categories = {'Todos'};
    for (var attraction in _attractions) {
      categories.add(attraction.category);
    }
    return categories.toList();
  }

  AttractionsProvider() {
    _loadSampleAttractions();
  }

  void _loadSampleAttractions() {
    _attractions = [
      Attraction(
        id: '1',
        name: 'Instituto Ricardo Brennand',
        description:
            'Um museu deslumbrante em estilo castelo que abriga uma das maiores coleções de arte brasileira e europeia da América Latina.',
        history:
            'Fundado em 2002 pelo empresário Ricardo Brennand, este complexo museológico apresenta um castelo em estilo medieval cercado por jardins. Abriga uma extensa coleção de arte brasileira e europeia, incluindo obras de Frans Post, o primeiro artista europeu a pintar paisagens brasileiras.',
        importance:
            'O Instituto Ricardo Brennand é uma das instituições culturais mais importantes do Brasil, mostrando o rico patrimônio artístico de Pernambuco e do Brasil. A própria arquitetura do castelo é uma obra de arte, criando uma experiência cultural única.',
        imageUrl:
            'https://images.unsplash.com/photo-1565967511849-76a60a516170?w=800',
        category: 'Museu',
        rating: 4.8,
        visitDuration: 180,
        price: 30.0,
        location: const LatLng(-8.0394, -34.9176),
        tags: ['Arte', 'História', 'Arquitetura', 'Castelo'],
        address: 'Alameda Antônio Brennand, s/n - Várzea, Recife - PE, 50741-904',
        phoneNumber: '+55 81 2121-0352',
        website: 'https://www.institutoricardobrennand.org.br/',
        openingHours: {
          'Segunda-feira': 'Fechado',
          'Terça-feira': '13:00 - 17:00',
          'Quarta-feira': '13:00 - 17:00',
          'Quinta-feira': '13:00 - 17:00',
          'Sexta-feira': '13:00 - 17:00',
          'Sábado': '13:00 - 17:00',
          'Domingo': '13:00 - 17:00',
        },
      ),
      Attraction(
        id: '2',
        name: 'Museu Cais do Sertão',
        description:
            'Um museu interativo dedicado à cultura e história do Nordeste brasileiro, especialmente da região do sertão.',
        history:
            'Inaugurado em 2014, o museu está localizado na antiga área portuária do Recife e celebra a cultura do Nordeste brasileiro através de exposições interativas, música e instalações multimídia.',
        importance:
            'O museu preserva e promove o rico patrimônio cultural da região Nordeste, incluindo o legado de Luiz Gonzaga, o "Rei do Baião", e mostra a resiliência e criatividade do povo sertanejo.',
        imageUrl:
            'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800',
        category: 'Museu',
        rating: 4.6,
        visitDuration: 120,
        price: 10.0,
        location: const LatLng(-8.0594, -34.8716),
        tags: ['Cultura', 'Interativo', 'Música', 'História'],
        address: 'Armazém 10, Av. Alfredo Lisboa, s/n - Recife Antigo, Recife - PE, 50030-150',
        phoneNumber: '+55 81 3182-8268',
        website: 'https://www.museucaisdosertao.pe.gov.br/',
        openingHours: {
          'Segunda-feira': 'Fechado',
          'Terça-feira': '09:00 - 17:00',
          'Quarta-feira': '09:00 - 17:00',
          'Quinta-feira': '09:00 - 17:00',
          'Sexta-feira': '09:00 - 17:00',
          'Sábado': '09:00 - 17:00',
          'Domingo': '09:00 - 17:00',
        },
      ),
      Attraction(
        id: '3',
        name: 'Paço do Frevo',
        description:
            'Um centro cultural dedicado ao frevo, o enérgico estilo de dança e música que é a alma do Carnaval do Recife.',
        history:
            'Inaugurado em 2014, o Paço do Frevo está localizado em um edifício histórico restaurado e serve como museu vivo e centro cultural para o frevo, que foi declarado Patrimônio Cultural Imaterial da Humanidade pela UNESCO.',
        importance:
            'O frevo é a expressão cultural mais importante de Pernambuco, representando a alegria, criatividade e resiliência do povo do Recife. O centro preserva esta tradição e educa os visitantes sobre sua importância.',
        imageUrl:
            'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800',
        category: 'Centro Cultural',
        rating: 4.5,
        visitDuration: 90,
        price: 8.0,
        location: const LatLng(-8.0614, -34.8716),
        tags: ['Frevo', 'Dança', 'Música', 'Carnaval'],
        address: 'Praça do Arsenal da Marinha, s/n - Recife Antigo, Recife - PE, 50030-000',
        phoneNumber: '+55 81 3355-9500',
        website: 'https://pacodofrevo.org.br/',
        openingHours: {
          'Segunda-feira': 'Fechado',
          'Terça-feira': '09:00 - 17:00',
          'Quarta-feira': '09:00 - 17:00',
          'Quinta-feira': '09:00 - 17:00',
          'Sexta-feira': '09:00 - 17:00',
          'Sábado': '09:00 - 17:00',
          'Domingo': '09:00 - 17:00',
        },
      ),
      Attraction(
        id: '4',
        name: 'Teatro de Santa Isabel',
        description:
            'Um teatro neoclássico construído em 1850, um dos teatros mais belos e históricos do Brasil.',
        history:
            'Construído entre 1841 e 1850, o Teatro de Santa Isabel é um dos teatros mais antigos do Brasil. Foi projetado pelo arquiteto francês Louis Léger Vauthier e já recebeu inúmeras apresentações, incluindo as de famosos artistas brasileiros.',
        importance:
            'O teatro é um símbolo da sofisticação cultural e do patrimônio artístico do Recife. Continua sendo um importante local para apresentações de música clássica, teatro e dança, mantendo seu papel como marco cultural.',
        imageUrl:
            'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800',
        category: 'Teatro',
        rating: 4.7,
        visitDuration: 60,
        price: 0.0,
        location: const LatLng(-8.0634, -34.8736),
        tags: ['Teatro', 'Arquitetura', 'Música Clássica', 'História'],
        address: 'Praça da República, s/n - Santo Antônio, Recife - PE, 50020-000',
        phoneNumber: '+55 81 3355-3323',
        website: 'https://www.teatrosantaisabel.com.br/',
        openingHours: {
          'Segunda-feira': '09:00 - 17:00',
          'Terça-feira': '09:00 - 17:00',
          'Quarta-feira': '09:00 - 17:00',
          'Quinta-feira': '09:00 - 17:00',
          'Sexta-feira': '09:00 - 17:00',
          'Sábado': '09:00 - 17:00',
          'Domingo': '09:00 - 17:00',
        },
      ),
      Attraction(
        id: '5',
        name: 'Centro de Artesanato de Pernambuco',
        description:
            'Uma vitrine do artesanato tradicional de Pernambuco, desde cerâmica e marcenaria até têxteis e rendas.',
        history:
            'O centro foi criado para preservar e promover o artesanato tradicional de Pernambuco, reunindo artesãos de todo o estado para expor e vender seus trabalhos.',
        importance:
            'O centro preserva técnicas artesanais tradicionais passadas de geração em geração e proporciona oportunidades econômicas para artesãos locais, mantendo essas tradições culturais vivas.',
        imageUrl:
            'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800',
        category: 'Centro de Artesanato',
        rating: 4.3,
        visitDuration: 60,
        price: 0.0,
        location: const LatLng(-8.0574, -34.8696),
        tags: ['Artesanato', 'Artesão', 'Tradicional', 'Compras'],
        address: 'Av. Alfredo Lisboa, s/n - Recife Antigo, Recife - PE, 50030-150',
        phoneNumber: '+55 81 3182-8268',
        website: 'https://www.artesanatodepernambuco.pe.gov.br/',
        openingHours: {
          'Segunda-feira': '09:00 - 18:00',
          'Terça-feira': '09:00 - 18:00',
          'Quarta-feira': '09:00 - 18:00',
          'Quinta-feira': '09:00 - 18:00',
          'Sexta-feira': '09:00 - 18:00',
          'Sábado': '09:00 - 18:00',
          'Domingo': '09:00 - 18:00',
        },
      ),
      Attraction(
        id: '6',
        name: 'Museu da Abolição',
        description:
            'Um museu dedicado a preservar a memória da escravidão e do movimento abolicionista no Brasil.',
        history:
            'Localizado em uma antiga mansão de traficante de escravos, o museu foi estabelecido para documentar e preservar a história da escravidão e do movimento abolicionista no Brasil, particularmente em Pernambuco.',
        importance:
            'O museu serve como um importante recurso educacional sobre a complexa história do Brasil com a escravidão e a luta contínua pela igualdade racial e justiça social.',
        imageUrl:
            'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800',
        category: 'Museu',
        rating: 4.4,
        visitDuration: 90,
        price: 0.0,
        location: const LatLng(-8.0554, -34.8756),
        tags: ['História', 'Educação', 'Memória', 'Justiça Social'],
        address: 'Rua Benfica, 1150 - Madalena, Recife - PE, 50720-001',
        phoneNumber: '+55 81 3228-3248',
        website: 'https://www.museudaabolicao.gov.br/',
        openingHours: {
          'Segunda-feira': 'Fechado',
          'Terça-feira': '09:00 - 17:00',
          'Quarta-feira': '09:00 - 17:00',
          'Quinta-feira': '09:00 - 17:00',
          'Sexta-feira': '09:00 - 17:00',
          'Sábado': '09:00 - 17:00',
          'Domingo': '09:00 - 17:00',
        },
      ),
    ];
    _filteredAttractions = _attractions;
    notifyListeners();
  }

  void filterByCategory(String category) {
    _selectedCategory = category;
    _applyFilters();
  }

  void searchAttractions(String query) {
    _searchQuery = query;
    _applyFilters();
  }

  void _applyFilters() {
    _filteredAttractions = _attractions.where((attraction) {
      bool matchesCategory = _selectedCategory == 'Todos' ||
          attraction.category == _selectedCategory;
      bool matchesSearch = _searchQuery.isEmpty ||
          attraction.name.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          attraction.description
              .toLowerCase()
              .contains(_searchQuery.toLowerCase()) ||
          attraction.tags.any(
              (tag) => tag.toLowerCase().contains(_searchQuery.toLowerCase()));

      return matchesCategory && matchesSearch;
    }).toList();

    notifyListeners();
  }

  Attraction? getAttractionById(String id) {
    try {
      return _attractions.firstWhere((attraction) => attraction.id == id);
    } catch (e) {
      return null;
    }
  }

  List<Attraction> getAttractionsNearby(
      LatLng location, double radiusInMeters) {
    return _attractions.where((attraction) {
      double distance = _calculateDistance(location, attraction.location);
      return distance <= radiusInMeters;
    }).toList();
  }

  double _calculateDistance(LatLng point1, LatLng point2) {
    const double earthRadius = 6371000; // Earth's radius in meters

    double lat1Rad = point1.latitude * (math.pi / 180);
    double lat2Rad = point2.latitude * (math.pi / 180);
    double deltaLatRad = (point2.latitude - point1.latitude) * (math.pi / 180);
    double deltaLonRad =
        (point2.longitude - point1.longitude) * (math.pi / 180);

    double a = math.sin(deltaLatRad / 2) * math.sin(deltaLatRad / 2) +
        math.cos(lat1Rad) *
            math.cos(lat2Rad) *
            math.sin(deltaLonRad / 2) *
            math.sin(deltaLonRad / 2);
    double c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));

    return earthRadius * c;
  }

  void clearFilters() {
    _selectedCategory = 'Todos';
    _searchQuery = '';
    _filteredAttractions = _attractions;
    notifyListeners();
  }
}
