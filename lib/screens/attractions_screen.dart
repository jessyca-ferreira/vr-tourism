import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:tourist_guide_app/providers/attractions_provider.dart';
import 'package:tourist_guide_app/providers/location_provider.dart';
import 'package:tourist_guide_app/screens/attraction_detail_screen.dart';
import 'package:tourist_guide_app/widgets/attraction_card.dart';
import 'package:tourist_guide_app/utils/theme.dart';

class AttractionsScreen extends StatefulWidget {
  const AttractionsScreen({super.key});

  @override
  State<AttractionsScreen> createState() => _AttractionsScreenState();
}

class _AttractionsScreenState extends State<AttractionsScreen> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: Consumer2<AttractionsProvider, LocationProvider>(
        builder: (context, attractionsProvider, locationProvider, child) {
          return CustomScrollView(
            slivers: [
              // App Bar
              SliverAppBar(
                expandedHeight: 120,
                floating: false,
                pinned: true,
                backgroundColor: AppTheme.surfaceColor,
                elevation: 0,
                flexibleSpace: FlexibleSpaceBar(
                  title: const Text(
                    'Atrações Artísticas do Recife',
                    style: TextStyle(
                      color: AppTheme.textPrimaryColor,
                      fontWeight: FontWeight.bold,
                      fontFamily: 'Poppins',
                    ),
                  ),
                  background: Container(
                    decoration: const BoxDecoration(
                      gradient: AppTheme.primaryGradient,
                    ),
                  ),
                ),
              ),

              // Search Bar
              SliverToBoxAdapter(
                child: Container(
                  padding: const EdgeInsets.all(16),
                  child: TextField(
                    controller: _searchController,
                    onChanged: attractionsProvider.searchAttractions,
                    decoration: InputDecoration(
                      hintText: 'Pesquisar atrações artísticas do Recife...',
                      prefixIcon: const Icon(Icons.search, color: AppTheme.textSecondaryColor),
                      suffixIcon: _searchController.text.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear, color: AppTheme.textSecondaryColor),
                              onPressed: () {
                                _searchController.clear();
                                attractionsProvider.searchAttractions('');
                              },
                            )
                          : null,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide.none,
                      ),
                      filled: true,
                      fillColor: AppTheme.surfaceColor,
                    ),
                  ),
                ),
              ),

              // Category Filter
              SliverToBoxAdapter(
                child: Container(
                  height: 50,
                  margin: const EdgeInsets.symmetric(horizontal: 16),
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    itemCount: attractionsProvider.categories.length,
                    itemBuilder: (context, index) {
                      final category = attractionsProvider.categories[index];
                      final isSelected = category == attractionsProvider.selectedCategory;
                      
                      return Container(
                        margin: const EdgeInsets.only(right: 8),
                        child: FilterChip(
                          label: Text(
                            category,
                            style: TextStyle(
                              color: isSelected ? Colors.white : AppTheme.textPrimaryColor,
                              fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                            ),
                          ),
                          selected: isSelected,
                          onSelected: (selected) {
                            attractionsProvider.filterByCategory(category);
                          },
                          backgroundColor: AppTheme.surfaceColor,
                          selectedColor: AppTheme.primaryColor,
                          checkmarkColor: Colors.white,
                          side: BorderSide(
                            color: isSelected ? AppTheme.primaryColor : AppTheme.textSecondaryColor.withOpacity(0.3),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ),

              // Results Count
              SliverToBoxAdapter(
                child: Container(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '${attractionsProvider.filteredAttractions.length} atrações encontradas',
                        style: AppTheme.body2.copyWith(
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      if (attractionsProvider.selectedCategory != 'Todos' || attractionsProvider.searchQuery.isNotEmpty)
                        TextButton(
                          onPressed: attractionsProvider.clearFilters,
                          child: const Text('Limpar filtros'),
                        ),
                    ],
                  ),
                ),
              ),

              // Attractions List
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                sliver: attractionsProvider.filteredAttractions.isEmpty
                    ? SliverToBoxAdapter(
                        child: Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                Icons.search_off,
                                size: 64,
                                color: AppTheme.textSecondaryColor.withOpacity(0.5),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                'Nenhuma atração encontrada',
                                style: AppTheme.heading3.copyWith(
                                  color: AppTheme.textSecondaryColor,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                'Tente ajustar sua pesquisa ou filtros',
                                style: AppTheme.body2,
                              ),
                            ],
                          ),
                        ),
                      )
                    : SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            final attraction = attractionsProvider.filteredAttractions[index];
                            return AttractionCard(
                              attraction: attraction,
                              distance: locationProvider.calculateDistance(attraction.location),
                              onTap: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => AttractionDetailScreen(
                                      attraction: attraction,
                                    ),
                                  ),
                                );
                              },
                            );
                          },
                          childCount: attractionsProvider.filteredAttractions.length,
                        ),
                      ),
              ),

              // Bottom padding
              const SliverToBoxAdapter(
                child: SizedBox(height: 20),
              ),
            ],
          );
        },
      ),
    );
  }
} 