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
		var errorMargin = document.getElementById("error-margin").value;
		if (sequences.length < 1) {
			alert("Need atleast one sequence!")
			return;
		}

		$.ajax({
			url: "http://130.211.79.61:8000/test?sequences=" + sequences.substr(0, sequences.length - 1) + "&error-margin=errorMargin",
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
			<table>
				<tr>
					<td width="40%">
						<div>
							{inputSequences}
							<br />
							<br />
							Alignment sequence:<br />
							{alignmentSequence}
							<br />
							<br />
							Error margin:<br />
							{errorMargin}
							<br />
							<br />
							<Button name="Produce graph and align" onClick={this.onClick}/>
						</div>
					</td>
					<td>
						<img src={src}/>
					</td>
				</tr>
			</table>
		)
	}
});

var Input = React.createClass({
	render: function () {
		console.log("Name: " + this.props.name);
		return (
			<div><input type="text" id={this.props.name}/> < br/>
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