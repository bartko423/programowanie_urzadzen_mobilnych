# FOX ADVENTURE to dwuwymiarowa platformówka inspirowana klasycznymi rozwiązaniami z serii Super Mario.
Projekt skupia się na:

    precyzyjnej fizyce skoku,

    czytelnych hitboxach,

    płynnych animacjach sprite’ów,

    prostym, responsywnym sterowaniu mobilnym,

    klasycznym układzie poziomów z platformami statycznymi i ruchomymi.

Celem było stworzenie lekkiego, przejrzystego silnika platformówki, który można łatwo rozwijać o nowe mechaniki.
 Użyte technologie
Android / Kotlin

Cała gra została napisana w Kotlinie, korzystając z:

    SurfaceView — ręczne rysowanie i pełna kontrola nad pętlą gry,

    Canvas — renderowanie sprite’ów i obiektów,

    MediaPlayer — muzyka w tle,

    własnej pętli gry (GameThread) — stabilne 60 FPS.

To podejście daje pełną kontrolę nad wydajnością i zachowaniem gry, bez narzucania ograniczeń silników typu Unity.
Grafika i animacje
PixelLab

Do tworzenia i skalowania animacji sprite’ów użyłem narzędzia PixelLab, które pozwoliło:

    generować spójne zestawy klatek animacji (idle, run, jump),

    zachować jednolity styl pixel‑art,

    łatwo eksportować sekwencje klatek do Androida.

Własny system animacji

Każda animacja to lista bitmap, przełączana w zależności od:

    stanu gracza (idle / run / jump),

    kierunku patrzenia,

    prędkości ruchu.

System jest lekki, prosty i w pełni kontrolowany z poziomu kodu.
Dlaczego takie rozwiązania?
1. Wzorowanie na Mario — sprawdzone mechaniki

Projekt bazuje na klasycznych rozwiązaniach z Mario, ponieważ:

    fizyka skoku jest intuicyjna i przewidywalna,

    kamera śledzi gracza, utrzymując go w centrum,

    platformy ruchome dodają dynamiki,

    monety prowadzą gracza przez poziom,

    hitbox jest mniejszy niż sprite — jak w Mario, aby gra była „uczciwa”.

To mechaniki, które od lat działają i są naturalne dla graczy.
2. SurfaceView zamiast silnika

Wybrałem SurfaceView, ponieważ:

    daje pełną kontrolę nad rysowaniem,

    pozwala stworzyć własny mini‑silnik,

    jest bardzo wydajny na Androidzie,

    idealnie nadaje się do gier 2D.

To świetna baza do nauki i dalszego rozwoju.
3. Własna fizyka i kolizje

Zamiast gotowych bibliotek:

    fizyka jest w pełni deterministyczna,

    kolizje są przewidywalne i łatwe do debugowania,

    kod jest prosty i rozszerzalny.

## Źródła i inspiracje

Podczas tworzenia projektu korzystałem z różnych materiałów dotyczących fizyki platformówek, animacji 2D oraz programowania gier na Androida. Najważniejsze z nich:

- https://developer.android.com/reference/android/view/SurfaceView  
- https://developer.android.com/reference/android/graphics/Canvas  
- https://developer.android.com/guide/topics/graphics/2d-graphics  
- https://developer.android.com/training/game-controllers/game-loop  
- https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection  
- https://www.gamedeveloper.com/design/the-guide-to-creating-platformer-physics  
- https://www.gamasutra.com/view/feature/131790/the_physics_of_platformers.php  
- https://www.mariowiki.com/Physics  
- https://www.piskelapp.com  
- https://play.google.com/store/apps/details?id=com.imaginstudio.imagetools.pixellab  
- https://www.slynyrd.com/blog/2018/1/10/pixel-logic-animating  
- https://gameprogrammingpatterns.com  
- https://www.gamedeveloper.com/design/level-design-lessons-from-super-mario-bros
