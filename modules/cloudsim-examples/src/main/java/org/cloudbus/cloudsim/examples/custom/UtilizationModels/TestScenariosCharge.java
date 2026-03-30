package org.cloudbus.cloudsim.examples.custom.UtilizationModels;

import org.cloudbus.cloudsim.UtilizationModel;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

public class TestScenariosCharge {

    public static void main(String[] args) {
        System.out.println("=== Test et Génération du Graphique des Modèles CPU ===");

        // 1. Initialisation des modèles
        UtilizationModelStatic modeleStatique = new UtilizationModelStatic(0.70);
        UtilizationModelCyclic modeleCyclique = new UtilizationModelCyclic(0.10, 0.90, 24);
        UtilizationModelBurst modeleBurst = new UtilizationModelBurst(0.20, 1.00, 50, 5);

        // 2. Préparation des tableaux de données pour XChart (101 points : de 0 à 100)
        int nombreDePoints = 101;
        double[] xTemps = new double[nombreDePoints];
        double[] yStatique = new double[nombreDePoints];
        double[] yCyclique = new double[nombreDePoints];
        double[] yBurst = new double[nombreDePoints];

        // 3. Remplissage des données
        for (int temps = 0; temps < nombreDePoints; temps++) {
            xTemps[temps] = temps;
            yStatique[temps] = modeleStatique.getUtilization(temps);
            yCyclique[temps] = modeleCyclique.getUtilization(temps);
            yBurst[temps] = modeleBurst.getUtilization(temps);
        }

        // 4. Création et configuration du graphique XChart
        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Comparaison des Scénarios de Charge")
                .xAxisTitle("Temps (ticks de simulation)")
                .yAxisTitle("Utilisation CPU (0.0 à 1.0)")
                .build();

        // Forcer l'axe Y à aller de 0 à 1.1 pour bien voir les pics à 100%
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(1.1);
        chart.getStyler().setLegendVisible(true);

        // 5. Ajout des séries de données au graphique
        XYSeries serieStatique = chart.addSeries("Charge Statique (70%)", xTemps, yStatique);
        serieStatique.setMarker(SeriesMarkers.NONE); // Désactiver les points pour avoir des lignes continues

        XYSeries serieCyclique = chart.addSeries("Charge Cyclique (Jour/Nuit)", xTemps, yCyclique);
        serieCyclique.setMarker(SeriesMarkers.NONE);

        XYSeries serieBurst = chart.addSeries("Charge Burst (Pics soudains)", xTemps, yBurst);
        serieBurst.setMarker(SeriesMarkers.NONE);

        // 6. Affichage du graphique dans une fenêtre
        System.out.println("Génération de la fenêtre graphique en cours...");
        new SwingWrapper<>(chart).displayChart();
    }
}