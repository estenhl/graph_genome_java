var Demo = React.createClass({
	getInitialState: function () {
		return {image: undefined};
	},
	renderInputSequences: function () {
		var rows = [];
		for (var i = 0; i < 10; i++) {
			var name = "Sequence_" + i;
			rows.push(<Input name={name} key={name}/>);
		}
		return <div>Input Sequences:<br />{rows}</div>;
	},
	renderAlignmentSequence: function () {
		return <Input name="alignment"/>
	},
	renderErrorMargin: function () {
		return <Select name="select"/>
	},
	onClick: function () {
		var sequences = "";
		for (var i = 0; i < 10; i++) {

			var sequence = document.getElementById("Sequence_" + i).value;
			if (sequence.length > 0) {
				sequences += sequence + ",";
			}
		}
		var alignment = document.getElementById("alignment").value;
		var e = document.getElementById("error-margin");
		var errorMargin = e.options[e.selectedIndex].value;
		if (sequences.length < 1) {
			alert("Need atleast one sequence!")
			return;
		}
		var url = "http://130.211.79.61:8000/test?sequences=" + sequences.substr(0, sequences.length - 1) + "&error-margin=" + errorMargin;
		if (alignment != undefined && alignment.length > 0) {
			url += "&alignment=" + alignment;
		}

		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'jsonp',
			jsonpCallback: 'mycallback',
			context: this,
			error: function (xhr, status, error) {
				console.log("xhr: " + xhr);
				console.log("status: " + status);
				console.log("error: " + error);
				alert("error");
			},
			success: function (json) {
				this.setState({image: json.png})
			}
		});
	},
	render: function () {
		console.log("Rendering DEMO");
		var inputSequences = this.renderInputSequences();
		var alignmentSequence = this.renderAlignmentSequence();
		var errorMargin = this.renderErrorMargin();
		var src = ""
		if (this.state.image) {
			src = "data:image/jpeg;base64," + this.state.image;
		}

		return (
			<table style={{backgroundColor: "lightblue", margin: "10px", marginTop: "30px", borderRadius: "10px", width: "99%"}}>
				<tr>
					<td class="col-md-12" style={{textAlign: "center"}}>
						<h1>Graph Genome Tool demo alpha v0.1</h1>
					</td>
				</tr>
				<tr>
					<table style={{padding: "5px"}}>
						<tr>
					<td style={{border: "1px solid black", width: "30%"}}>
						(DISCLAIMER: This is a very small demo of the tool found in https://github.com/estenpro/graph_genome_java. For larger sequences and more flexibility follow the download and execute instructions for the command line tool)<br />
						<br />
						{inputSequences}
						<br />
						<br />
						Alignment sequence:<br />
						{alignmentSequence}
						<br />
						<font color="red">(IMPORTANT DISCLAIMER: In order to achieve "expected" results, this parameter must be set to atleast the smallest edit distance between the strings)</font><br />
						Error margin:{errorMargin}
						<br />
						<br />
						<Button name="Test!" onClick={this.onClick}/>
					</td>
					<td style={{width: "70%"}}>
						Result: <br />
						<img src={src}/>
					</td>
				</tr>
						</table></tr>
			</table>
		)
	}
});

var Input = React.createClass({
	render: function () {
		console.log("Name: " + this.props.name);
		return (
			<div><input type="text" id={this.props.name} style={{marginBottom: "2px", borderRadius: "2px"}}/> < br/>
			</div>
		)
	}
});

var Select = React.createClass({
	renderOptions: function () {
		var rows = [];
		for (var i = 0; i < 10; i++) {
			rows.push(<option value={i}>{i}</option>);
		}
		return (<select defaultValue={3} id="error-margin">{rows}</select>);
	},
	render: function () {
		var options = this.renderOptions();
		return (
			<div>
				{options}
			</div>
		)
	}
});

var Button = React.createClass({
	render: function () {
		return (
			<button onClick={this.props.onClick}>{this.props.name}</button>
		);
	}
});

ReactDOM.render(
	<Demo />,
	document.getElementById('example')
);