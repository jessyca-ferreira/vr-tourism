import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:tourist_guide_app/providers/location_provider.dart';
import 'package:tourist_guide_app/providers/attractions_provider.dart';
import 'package:tourist_guide_app/screens/home_screen.dart';
import 'package:tourist_guide_app/utils/theme.dart';

void main() {
  runApp(const TouristGuideApp());
}

class TouristGuideApp extends StatelessWidget {
  const TouristGuideApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => LocationProvider()),
        ChangeNotifierProvider(create: (_) => AttractionsProvider()),
      ],
      child: MaterialApp(
        title: 'Recife Art Guide',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        home: const HomeScreen(),
      ),
    );
  }
} 