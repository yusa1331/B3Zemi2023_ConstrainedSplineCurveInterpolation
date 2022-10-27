package jp.sagalab.b3semi;

import jp.sagalab.b3semi.io.csv.ReadCSV;
import jp.sagalab.b3semi.io.csv.WriteToCSV;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yako, takashima, inagaki
 */
public class Main extends JFrame {

  /**
   * @param _args the command line arguments
   */
  public static void main(String[] _args) {
    new Main();
  }

  /**
   * コンストラクタ
   */
  public Main() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    m_canvas.setSize(800, 600);
    m_canvas.setBackground(Color.WHITE);
    setTitle("b3zemi");

    // 入力した点列を保存する"save points"ボタン
    JButton savePoints = new JButton("save points");
    // ボタンを押したときの処理
    savePoints.addActionListener(e -> WriteToCSV.writePoints(m_points));

    // 点列を読み込むための"load points"ボタン
    JButton loadPoints = new JButton("load points");
    // ボタンを押したときの処理
    loadPoints.addActionListener(e -> {
      // 読み込みたい点列のCSVファイルのパスをfilePathに入れる.
      String filePath = ".\\Points\\samplePoints.csv";
      File pointsFile = new File(filePath);
      // readPointsメソッドを用いてPointのリストに変換
      List<Point> points = ReadCSV.readPoints(pointsFile);
      // m_Pointsを上書き
      setPoints(points);
      // 点列を描画
      for (Point point : points) {
        drawPoint(point.x(), point.y());
      }
      // スプライン曲線を描画
      drawSplineCurve();
    });

    add(m_canvas, BorderLayout.CENTER);
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(savePoints);
    buttonPanel.add(loadPoints);
    add(buttonPanel, BorderLayout.SOUTH);
    pack();
    setVisible(true);

    // ドラッグする場合マウスのボタン入力は,
    // mousePressed → mouseDragged → mouseReleased の順で処理が行われる.
    // またドラッグ中はマウスカーソルが動くたびにmouseDraggedが呼び出される.
    m_canvas.addMouseListener(
            new MouseAdapter() {
              @Override
              public void mousePressed(MouseEvent e) {
                clear();
                m_points.clear();
              }

              @Override
              public void mouseReleased(MouseEvent e) {
                drawSplineCurve();
              }
            }
    );

    m_canvas.addMouseMotionListener(
            new MouseMotionAdapter() {
              @Override
              public void mouseDragged(MouseEvent e) {
                Point point = Point.createXYT(e.getX(), e.getY(), System.currentTimeMillis() * 0.001);
                m_points.add(point);
                drawPoint(point.x(), point.y());
              }
            }
    );
  }

  /**
   * スプライン曲線を求め、描画を行う.
   */
  public void drawSplineCurve() {

    // 点列の時系列を0始まりにシフトする.
    List<Point> shiftedPoints = shiftTimeToZero(m_points);
    // リストを配列に変換する.
    Point[] points = shiftedPoints.toArray(new Point[0]);

    // knotを指定しない場合↓ (節点間隔に合わせて節点列を自動で生成)
    // SplineCurveInterpolator.interpolateの最後の引数は節点間隔(double型)
    // 次数
    int degree = 3;
    // 節点間隔
    double knotInterval = 0.1;
    double[] knot = ReadCSV.readKnots(new File(".\\Knot\\sampleKnots.csv"));
    // スプライン補間を行う
    SplineCurve splineCurve = SplineCurveInterpolator.interpolate(points, degree, knotInterval);

//    // knotを指定する場合↓
//    // SplineCurveInterpolator.interpolateの最後の引数が節点列(double[]型)
//    // 次数
//    int degree = 3;
//    // 節点列は下のように自分で指定したり, double[] knot = ReadCSV.readKnots(new File(".\\Knot\\sampleKnots.csv"));
//    // のようにCSVファイルから持ってきたりできるのでいろいろ試してみると良い.
//    double[] knot = new double[] {-0.4, -0.2, 0.0, 0.2, 0.4, 0.8, 1.2, 1.6, 1.9, 2.1, 2.3};
//    // スプライン補間を行う
//    SplineCurve splineCurve = SplineCurveInterpolator.interpolate(points, degree, knot);


    double start = splineCurve.range().start();
    double end = splineCurve.range().end();
    List<Point> evaluateList = new ArrayList<>();
    // 評価点を求める
    for (double t = start; t < end; t += 0.01) {
      evaluateList.add(splineCurve.evaluate(t));
    }

    // SplineCurveの描画
    for (int i = 1; i < evaluateList.size(); i++) {
      drawLine(evaluateList.get(i-1), evaluateList.get(i), Color.RED);
    }
  }

  /**
   * 点を描画する.
   *
   * @param _x x座標
   * @param _y y座標
   */
  public void drawPoint(double _x, double _y) {
    Graphics2D g = (Graphics2D) m_canvas.getGraphics();
    double radius = 1.5;

    Ellipse2D.Double oval = new Ellipse2D.Double(_x - radius, _y - radius, radius * 2, radius * 2);
    g.draw(oval);
  }

  /**
   * 線を描画する.
   *
   * @param _p1 始点
   * @param _p2 終点
   */
  public void drawLine(Point _p1, Point _p2, Color _color) {
    Graphics2D g = (Graphics2D)m_canvas.getGraphics();
    g.setColor(_color);

    Line2D.Double line = new Line2D.Double(_p1.x(), _p1.y(), _p2.x(), _p2.y());
    g.draw(line);
  }

  /**
   * 描画を初期化する.
   */
  public void clear() {
    Graphics g = m_canvas.getGraphics();

    g.clearRect(0, 0, m_canvas.getWidth(), m_canvas.getHeight());
  }

  public static List<Point> shiftTimeToZero(List<Point> _points) {
    double startTime = _points.get(0).time();
    List<Point> points = new ArrayList<>();
    for (Point point : _points) {
      points.add(Point.createXYT(point.x(), point.y(), point.time() - startTime));
    }
    return points;
  }

  /**
   * m_pointsを引数の点列で上書きする.
   *
   * @param _points 点列
   */
  public void setPoints(List<Point> _points) {
    m_points = new ArrayList<>(_points);
  }

  /** キャンバスを表す変数 */
  private final Canvas m_canvas = new Canvas();

  /** ドラッグで打たれた点列を保持するリスト */
  private List<Point> m_points = new ArrayList<>();
}