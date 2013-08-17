import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;


public class MyDiagnosticListener<S> implements DiagnosticListener<S> {

	@Override
	public void report(Diagnostic<? extends S> diagnostic) {
		System.err.println(diagnostic.getMessage(null));
		return;
	}

}
